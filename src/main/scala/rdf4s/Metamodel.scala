package rdf4s

import com.typesafe.scalalogging.LazyLogging
import org.openrdf.model.vocabulary.RDF.TYPE
import org.openrdf.model.vocabulary.RDFS.SUBCLASSOF
import org.openrdf.model.{IRI, Model, Resource, Value}
import rdf4s.Metamodel._
import rdf4s.implicits._

import scala.reflect.runtime.universe._
import scala.reflect.runtime.{currentMirror => mirror}


object Metamodel {
    type LookupFn = Resource => Option[AnyRef]

    val mmNamespace = "mm:"
    val mmMapped = iri(mmNamespace, "mapped")
    val mmCtor = iri(mmNamespace, "ctor")
    val mmCtorParam = iri(mmNamespace, "ctorParam")

    val mmProperty = iri(mmNamespace, "RdfProperty")
    val mmPropertyGet = iri(mmNamespace, s"${mmProperty.getLocalName}Get")
    val mmPropertySet = iri(mmNamespace, s"${mmProperty.getLocalName}Set")
}


// the requirement of TypeTag may get dropped back to ClassTag
// prototyping a metamodel for use by rdf4s
// this will likely be a separate project from the actual rdf library
class Metamodel extends LazyLogging {
    val mmodel = model()
    val mmap = collection.mutable.Map[Resource, AnyRef]()
    val tmap = collection.mutable.Map[Class[_], Resource]()

    initBuiltins()

    def query[R](t: Class[_], q: (Resource, Model, LookupFn) => R): Option[R] = tmap.get(t).map(r => q(r, mmodel, mmap.get))

    // another construction potential; noting the access of the symbol from the fqcn
    def fromname(fqcn: String) = {
        val clss = Class.forName(fqcn)
        val s = mirror.classSymbol(clss)
        s.info.members
    }

    // install is temporary, eventually metamodels are immutable and created with a builder
    def install[T: TypeTag](): Unit = {
        val (tid, tpe) = relateType[T]

        tpe.members
        .filter(_.isTerm).map(_.asTerm)
        .filter(x => x.isVal || x.isVar)
        .map { x => x -> (symOrNone(x.getter) -> symOrNone(x.setter)) }
        .foreach { a =>
            val aid = relate(a._1, mmProperty, tid)
            a._2._1.foreach(relate(_, mmPropertyGet, aid))
            a._2._2.foreach(relate(_, mmPropertySet, aid))
        }

        tpe.members
        .filter(_.isConstructor)
        .map(_.asMethod)
        .map(c => c -> relate(c, mmCtor, tid))
        .foreach { t =>
            t._1.paramLists.head.foreach(relate(_, mmCtorParam, t._2))
        }
    }

    // pattern is {thing} {relation} {owner}
    private def relate(sym: Symbol, p: IRI, o: IRI, lnf: => String = random()): IRI = {
        val s = iriOf(sym).getOrElse(iri(mmNamespace, lnf))
        mmap(s) = sym
        add(s, p, o)
        add(s, SUBCLASSOF, p)
        logger.trace(s"rel [${sym.owner.fullName}.$sym] as { s[$s] p[$p] o[$o] }")
        s
    }

    private def relateType[T: TypeTag]: (IRI, Type) = relateType[T](random())
    private def relateType[T: TypeTag](lnf: => String): (IRI, Type) = {
        val tpe = typeTag[T].tpe
        val tid = relate(tpe.typeSymbol, TYPE, mmMapped, lnf)
        tmap(mirror.runtimeClass(tpe)) = tid
        tid -> tpe
    }

    private def symOrNone(s: Symbol): Option[Symbol] = if (s != NoSymbol) Option(s) else None
    private def add(s: Resource, p: IRI, o: Value, g: Option[Resource] = None) = mmodel.add(statement(s, p, o, g))

    private val irianno = typeOf[useIRI]
    private def iriOf(s: Symbol): Option[IRI] = s.annotations.find(_.tree.tpe == irianno).map(bigHackForAnnoParam).map(iri)

    // need to sort out the proper way to query the AST tree to get the param
    private def bigHackForAnnoParam(a: Annotation): String = a.tree.children.last.children.last.toString().replaceAll("\"", "")

    private def initBuiltins() = {
        relateType[Char]("Char")
        relateType[Byte]("Byte")
        relateType[Short]("Short")
        relateType[Int]("Int")
        relateType[Long]("Long")
        relateType[Float]("Float")
        relateType[Double]("Doublt")
        relateType[String]("String")
    }
}
