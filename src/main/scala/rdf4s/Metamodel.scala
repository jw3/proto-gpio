package rdf4s

import com.typesafe.scalalogging.LazyLogging
import org.openrdf.model.{IRI, Model, Resource, Value}

import scala.collection.JavaConversions._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}

import Metamodel._
import implicits._
import org.openrdf.model.vocabulary.RDF.TYPE
import org.openrdf.model.vocabulary.RDFS.SUBCLASSOF


object Metamodel {
    type LookupFn = Resource => Option[AnyRef]

    val mmNamespace = "mm:"
    val mmMapped = iri(mmNamespace, "mapped")
    val mmCtor = iri(mmNamespace, "ctor")
    val mmCtorParam = iri(mmNamespace, "ctorParam")

    val mmProperty = iri(mmNamespace, "property")
    val mmPropertyGet = iri(mmNamespace, s"${mmProperty.getLocalName}Get")
    val mmPropertySet = iri(mmNamespace, s"${mmProperty.getLocalName}Set")
}


// prototyping a metamodel for use by rdf4s
// this will likely be a separate project from the actual rdf library
class Metamodel extends LazyLogging {
    val mmodel = model()
    val mmap = collection.mutable.Map[Resource, AnyRef]()
    val rtmap = collection.mutable.Map[Class[_], Resource]()

    initBuiltins()

    def query[R](t: Class[_], q: (Resource, Model, LookupFn) => R): Option[R] = rtmap.get(t).map(r => q(r, mmodel, mmap.get(_)))

    def install[T: TypeTag] = {
        val (tid, tpe) = relateType[T]

        tpe.members
        .filter(_.isTerm)
        .map(_.asTerm)
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
    private def relate(sym: Symbol, p: IRI, o: IRI, lnf: => String = random): IRI = {
        val s = iri(mmNamespace, lnf)
        mmap(s) = sym
        add(s, p, o)
        add(s, SUBCLASSOF, p)
        logger.trace(s"rel [${sym.owner.fullName}.$sym] as { s[$s] p[$p] o[$o] }")
        s
    }

    private def relateType[T: TypeTag](): (IRI, Type) = relateType[T](random)
    private def relateType[T: TypeTag](lnf: => String): (IRI, Type) = {
        val tpe = typeTag[T].tpe
        val tid = relate(tpe.typeSymbol, TYPE, mmMapped, lnf)
        rtmap(ru.rootMirror.runtimeClass(tpe)) = tid
        (tid -> tpe)
    }

    private def symOrNone(s: Symbol): Option[Symbol] = if (s != NoSymbol) Option(s) else None
    private def add(s: Resource, p: IRI, o: Value, g: Option[Resource] = None) = mmodel.add(statement(s, p, o, g))

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
