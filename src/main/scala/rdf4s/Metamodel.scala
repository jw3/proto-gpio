package rdf4s

import org.openrdf.model.{IRI, Resource, Value}

import scala.collection.JavaConversions._
import scala.reflect.runtime.universe._

class Metamodel {
    import implicits._
    import org.openrdf.model.vocabulary.RDFS._

    val ns = "mm:"
    val beanType = ns + "bean"
    val beanCtor = ns + "tctor"
    val ctorParm = ns + "cprop"
    val beanProp = ns + "bprop"

    // needs to be threadsafe
    val mmap = collection.mutable.Map[IRI, AnyRef]()

    val mmodel = model()

    def single[T: TypeTag](t: T) = {
    }

    def cascade[T: TypeTag](t: T) = {
        val rmmap = collection.mutable.Map[AnyRef, IRI]()

        val ts = typeTag[T].tpe

        // add the type
        val tid = iri(ns, random)
        mmap(tid) = ts
        add(tid, SUBCLASSOF, beanType)


        val accessors = ts.members.map(_.asTerm).filter(x => x.isVal || x.isVar).map { x =>
            val setter = if (x.setter != NoSymbol) Option(x) else None
            (x -> (x.getter -> setter))
        }

        accessors.foreach { a =>
            val aid = iri(ns, random)
            mmap(aid) = a._1

            add(aid, beanType, tid)
            add(aid, SUBCLASSOF, beanProp)
        }

        val ctors = ts.members.filter(_.isConstructor).map(_.asMethod)
        ctors.foreach { c =>
            val ctorId = iri(ns, random)
            mmap(ctorId) = c
            rmmap(c) = ctorId

            // map the accessor to the bean
            add(ctorId, beanType, tid)
            add(ctorId, SUBCLASSOF, beanCtor)
        }

        val ctorParams = ctors.map(_.asMethod).map { c =>
            (c -> c.paramLists.head)
        }

        ctorParams.foreach { pv =>
            val ctorId = rmmap(pv._1)

            pv._2.foreach { p =>
                val pId = iri(ns, random)
                mmap(pId) = p

                // pid is a ctorparam and belongs to ctorid
                add(pId, ctorParm, ctorId)
                add(pId, SUBCLASSOF, ctorParm)
            }
        }
    }

    def add(s: Resource, p: IRI, o: Value, g: Option[Resource] = None) = mmodel.add(statement(s, p, o, g))

    class CachedMM {
    }
}

class Test1(name: String)

object testx {
    def main(args: Array[String]) {
        val t = new Test1("first-test")

        val mm = new Metamodel()
        mm.cascade(t)


        mm.mmodel.foreach(println)
        mm.mmap.foreach(println)
    }
}
