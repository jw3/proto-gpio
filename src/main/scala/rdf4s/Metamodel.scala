package rdf4s

import com.typesafe.scalalogging.LazyLogging
import org.openrdf.model.{IRI, Resource, Value}

import scala.collection.JavaConversions._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}

class Metamodel extends LazyLogging {
    import implicits._
    import org.openrdf.model.vocabulary.RDFS._

    val ns = "mm:"
    val beanType = ns + "bean"
    val beanClass = ns + "rtclass"
    val beanCtor = ns + "tctor"
    val ctorParm = ns + "cprop"

    val beanProp = ns + "bprop"
    val beanPropGet = beanProp + "Get"
    val beanPropSet = beanProp + "Set"

    // needs to be threadsafe
    val rtmap = collection.mutable.Map[Class[_], Resource]()
    val mmap = collection.mutable.Map[Resource, AnyRef]()

    val mmodel = model()


    // init built in types
    {
        val intTpe = typeTag[Int].tpe
        val intTid = iri(ns, "Int")
        mmap(intTid) = intTpe
        rtmap(ru.rootMirror.runtimeClass(intTpe)) = intTid
    }

    {
        val intTpe = typeTag[String].tpe
        val intTid = iri(ns, "String")
        mmap(intTid) = intTpe
        rtmap(ru.rootMirror.runtimeClass(intTpe)) = intTid
    }

    def sit1[T: TypeTag]: Option[Seq[MethodSymbol]] = {
        val t = ru.rootMirror.runtimeClass(typeOf[T])
        rtmap.get(t).map { tid =>
            mmodel.filter(null, beanCtor, tid).subjects
                .flatMap(r => mmap.get(r))
                .map(m => m.asInstanceOf[MethodSymbol])
                .toSeq
        }
    }

    // get all gets
    def sit2[T](t: Class[T]): Option[Seq[Symbol]] = {
        rtmap.get(t).map { tid =>
            mmodel.filter(null, beanProp, tid).subjects
                .flatMap(aid => mmodel.filter(null, beanPropGet, aid).subjects)
                .flatMap(r => mmap.get(r))
                .map { m => m.asInstanceOf[Symbol]
            }
                .toSeq
        }
    }

    // map of ctor ids to the types; not worrying about param ordering now
    // we return the ctorid, so if the param list is interesting the ctor can be obtained
    def ctors[T](t: Class[T]): Map[Resource, Seq[Symbol]] = {
        rtmap.get(t).map { tid =>
            mmodel.filter(null, beanCtor, tid).subjects
                .map(cid => cid -> mmodel.filter(null, ctorParm, cid).subjects)
                .map(m => m._1 -> m._2.map(mmap.get(_)))
                .filterNot(m => m._2.contains(None)) /* filter out entries that have params that are not mapped */
                .map(m => m._1 -> m._2.flatten)
                .map(m => m._1 -> m._2.map(_.asInstanceOf[Symbol]).toSeq)
                .toMap
        }.getOrElse(Map[Resource, Seq[Symbol]]())
    }

    def single[T: TypeTag](t: T) = {
    }

    def cascade[T: TypeTag](t: T) = {
        val rmmap = collection.mutable.Map[AnyRef, IRI]()

        val tpe = typeTag[T].tpe

        val tid = iri(ns, random)
        mmap(tid) = tpe
        rtmap(ru.rootMirror.runtimeClass(tpe)) = tid
        add(tid, SUBCLASSOF, beanType)

        val accessors = tpe.members
            .map(_.asTerm)
            .filter(x => x.isVal || x.isVar)
            .map(x => (x -> (
            (if (x.getter != NoSymbol) Option(x.getter) else None) ->
                (if (x.setter != NoSymbol) Option(x.setter) else None))))

        accessors.foreach { a =>
            val aid = iri(ns, random)
            mmap(aid) = a._1
            logger.trace(s"accessor [$aid] -> [${a._1}}]")

            add(aid, beanProp, tid)
            add(aid, SUBCLASSOF, beanProp)

            a._2._1.foreach { g =>
                val gid = iri(ns, random)
                mmap(gid) = g
                add(gid, beanPropGet, aid)
            }
            a._2._2.foreach { s =>
                val sid = iri(ns, random)
                mmap(sid) = s
                add(sid, beanPropSet, aid)
            }
        }

        val ctors = tpe.members.filter(_.isConstructor).map(_.asMethod)
        ctors.foreach { c =>
            val ctorId = iri(ns, random)
            mmap(ctorId) = c
            rmmap(c) = ctorId

            // map the accessor to the bean
            add(ctorId, beanCtor, tid)
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


    // pattern is {thing} {relation} {ownedby}


    def add(s: Resource, p: IRI, o: Value, g: Option[Resource] = None) = mmodel.add(statement(s, p, o, g))

    class CachedMM {
    }
}

class Test1(name: String, age: Int) {
    var x: String = _
}

object testx {
    def main(args: Array[String]) {
        val b = new Test1("bob", 99)

        val mm = new Metamodel()
        mm.cascade(b)

        //mm.mmap.foreach(println)

        // 1 start with a TypeTag
        // situation 1: query: get all instances of Foo
        println(mm.sit1[Test1])
        println(mm.sit1[String])


        // 2 start with an object
        // situation 2: write: given a Foo, serialize it
        println(mm.sit2(b.getClass))

        //3 start with type
        // figure out what ctors are available
        println(mm.ctors(classOf[Test1]))


        //        mm.mmodel.foreach(println)
        //        mm.mmap.foreach(println)
    }
}

/*
the semantics of orm and case classes (wonder how slick does it?)
    - you can populate any object, the post contstruct reading/writing capability of that object is not concern
        - so you may be able to read objects you cant write back
        - whats the use, maybe function objects?
    -

    * write to class parameters
    * write to setters
 */
