package rdf4s

import com.typesafe.scalalogging.LazyLogging
import org.openrdf.model.vocabulary.RDF.TYPE
import org.openrdf.model.vocabulary.{OWL, XMLSchema}
import org.openrdf.model.{IRI, Model, Resource, Value}
import rdf4s.Metamodel._
import rdf4s.implicits._

import scala.reflect._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{currentMirror => mirror}

object Metamodel {
    type LookupFn = Resource => Option[AnyRef]
    type QueryFn[R] = (Resource, Model, LookupFn) => R

    val mmNamespace = "mm:"
    val mmInstanceOf = prop("instanceOf")

    val mmBuiltin = prop("builtin")
    val mmMapped = prop("mapped")
    val mmCtor = prop("ctor")
    val mmCtorParam = prop("ctorParam")

    val mmProperty = prop("RdfProperty")
    val mmPropertyGet = prop(s"${mmProperty.getLocalName}Get")
    val mmPropertySet = prop(s"${mmProperty.getLocalName}Set")

    val mmAnnotation = prop("anno")
    val mmAnnotates = prop("annotates")
    val mmAnnotationProperty = prop("annoProp")
    val mmAnnotationValue = prop("annoPropVal")

    def ontology: Model = model { add =>
        add < mmBuiltin >< TYPE >< OWL.CLASS >;
        add < mmMapped >< TYPE >< OWL.CLASS >;
        add < mmAnnotation >< TYPE >< OWL.CLASS >;
    }

    def prop(ln: String): IRI = iri(mmNamespace, ln)
}


// prototyping a metamodel for use by rdf4s
// this will likely be a separate project from the actual rdf library
class Metamodel extends LazyLogging {
    val mmodel = model(ontology.toSeq())
    val mmap = collection.mutable.Map[Resource, AnyRef]()
    val smap = collection.mutable.Map[Symbol, Resource]()
    val tmap = collection.mutable.Map[Class[_], Resource]()

    initBuiltins()

    def query[R](t: Class[_], q: QueryFn[R]): Option[R] = tmap.get(t).map(r => q(r, mmodel, mmap.get))

    // support both registration from literals and from string fqcn
    def install(fqcn: String) = doInstall(ClassTag(Class.forName(fqcn)))
    def install[T: ClassTag]: Unit = doInstall(classTag[T])
    private def doInstall(c: ClassTag[_]): Unit = {
        val (tid, tpe) = registerType(mmMapped)(c)

        // this is not the right place for mapping the annotation type
        val annotations = tpe.typeSymbol.annotations
        annotations.foreach(a => registerAnno(a.tree.tpe.typeSymbol, tid))

        // map annotation to its contents (params as symbols)
        val annoMapping = annotations.map(a => a -> a.tree.tpe.typeSymbol.asClass.primaryConstructor.asMethod.paramLists.flatten)
                          // trade the anno for its symbol; zip up the content symbols with their trees
                          .map(ap => ap._1.tree.tpe.typeSymbol -> ap._2.zip(ap._1.tree.children.tail))

        // map the children trees to their values (literals only)
        val annoValues = annoMapping.map { z => z._1 -> z._2.map(v => v._1 -> collectValues(v._2)) }.map(x => x).toMap

        annoMapping.foreach { am =>
            val ainid = registerAnno(am._1, tid)
            annoValues(am._1).foreach { t =>
                val pid = bnode()
                add(pid, mmAnnotationProperty, ainid)
                add(pid, mmAnnotationValue, value(t._2))
            }
        }

        tpe.members
        .filter(_.isTerm).map(_.asTerm)
        .filter(x => x.isVal || x.isVar)
        .map { x => x -> (symOrNone(x.getter) -> symOrNone(x.setter)) }
        .foreach { a =>
            val aid = register(a._1, mmProperty)
            add(aid, mmProperty, tid)
            a._2._1.foreach { g =>
                register(g, mmPropertyGet)
                relate(g, mmPropertyGet, aid)
            }
            a._2._2.foreach { g =>
                register(g, mmPropertySet)
                relate(g, mmPropertySet, aid)
            }
        }

        tpe.members
        .filter(_.isConstructor)
        .map(_.asMethod)
        .map { c =>
            register(c, mmCtor)
            c -> relate(c, mmCtor, tid)
        }
        .foreach { t =>
            t._1.paramLists.head.foreach { c =>
                val cpid = register(c, mmCtorParam)
                add(cpid, mmCtorParam, tid)
            }
        }
    }

    private def relate(sym: Symbol, p: IRI, o: Value, lnf: => String = random()): Unit =
        smap.get(sym).map(s =>
            add(s, p, o))

    private def registerAnno(sym: Symbol, annotated: Resource, lnf: => String = random): Resource = {
        val aid = register(sym, mmAnnotation, lnf)
        val inid = bnode()
        add(inid, mmInstanceOf, aid)
        add(inid, mmAnnotates, annotated)
        ///logger.trace(s"$annotated is annotated with $aid")
        inid
    }
    private def registerType[T: ClassTag](`type`: Resource): (Resource, Type) = registerType[T](`type`, random())
    private def registerType[T: ClassTag](`type`: Resource, lnf: => String): (Resource, Type) = {
        val rtclass = classTag[T].runtimeClass
        val symbol = mirror.classSymbol(rtclass)
        val tid = register(symbol, `type`, lnf)
        tmap(rtclass) = tid
        tid -> symbol.info
    }

    private def register[T: ClassTag](`type`: Resource, lnf: => String): (Resource, Type) = {
        val rtclass = classTag[T].runtimeClass
        val symbol = mirror.classSymbol(rtclass)
        register(symbol, `type`, lnf) -> symbol.info
    }

    private def register(sym: Symbol, `type`: Resource, lnf: => String = random()): Resource = {
        smap.getOrElseUpdate(sym, {
            val s = iriOf(sym).getOrElse(iri(mmNamespace, lnf))
            mmap(s) = sym
            ///logger.trace(s"reg [${sym.owner.fullName}.$sym] as [$s]")
            add(s, TYPE, `type`)
            s
        })
    }

    private def symOrNone(s: Symbol): Option[Symbol] = if (s != NoSymbol) Option(s) else None
    private def add(s: Resource, p: IRI, o: Value, g: Option[Resource] = None): Unit = {
        mmodel.add(statement(s, p, o, g))
        logger.trace(s"add: { $s $p $o }")
    }

    private val irianno = typeOf[useIRI]
    private def iriOf(s: Symbol): Option[IRI] = s.annotations.find(_.tree.tpe == irianno).map(bigHackForIriOf).map(iri)

    // need to sort out the proper way to query the AST tree to get the param
    private def bigHackForIriOf(a: Annotation): String = a.tree.children.last.children.last.toString().replaceAll("\"", "")
    private def collectValues(t: Tree) = t match {
        case Literal(c) => rdf4s.value(c.value)
        case _ => logger.warn("unsupported annotation value")
    }


    private def initBuiltins() = {
        import XMLSchema._

        register[Byte](BYTE, "Byte")
        register[Short](SHORT, "Short")
        register[Int](INT, "Int")
        register[Long](LONG, "Long")
        register[Float](FLOAT, "Float")
        register[Double](DOUBLE, "Doublt")
        register[String](STRING, "String")
    }
}
