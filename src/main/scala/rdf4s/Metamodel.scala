package rdf4s

import com.typesafe.scalalogging.LazyLogging
import org.openrdf.model.{IRI, Resource, Value}

import scala.collection.JavaConversions._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}


// prototyping a metamodel for use by rdf4s
// this will likely be a separate project from the actual rdf library
class Metamodel extends LazyLogging {
    import implicits._
    import org.openrdf.model.vocabulary.RDF.TYPE
    import org.openrdf.model.vocabulary.RDFS.SUBCLASSOF

    val ns = "mm:"
    val mmMapped = ns + "mmMapped"
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

    relateType[Int]("Int")
    relateType[String]("String")


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
            }.toSeq
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

    def cascade[T: TypeTag] = {
        val rmmap = collection.mutable.Map[AnyRef, IRI]()

        val (tid, tpe) = relateType[T]
        add(tid, SUBCLASSOF, beanType)

        val accessors = tpe.members
            .map(_.asTerm)
            .filter(x => x.isVal || x.isVar)
            .map { x => x -> (symOrNone(x.getter) -> symOrNone(x.setter)) }

        accessors.foreach { a =>
            val aid = relate(a._1, beanProp, tid)
            a._2._1.foreach(relate(_, beanPropGet, aid))
            a._2._2.foreach(relate(_, beanPropSet, aid))
        }

        val ctors = tpe.members.filter(_.isConstructor).map(_.asMethod)
        ctors.foreach { c => rmmap(c) = relate(c, beanCtor, tid) }

        val ctorParams = ctors.map(_.asMethod).map { c =>
            (c -> c.paramLists.head)
        }

        ctorParams.foreach { pv =>
            val ctorId = rmmap(pv._1)
            pv._2.foreach(relate(_, ctorParm, ctorId))
        }
    }

    // pattern is {thing} {relation} {owner}
    def relate(sym: Symbol, p: IRI, o: IRI, lnf: => String = random): IRI = {
        val s = iri(ns, lnf)
        mmap(s) = sym
        add(s, p, o)
        add(s, SUBCLASSOF, p)
        logger.trace(s"rel [${sym.owner.fullName}.$sym] as { s[$s] p[$p] o[$o] }")
        s
    }

    def relateType[T: TypeTag](): (IRI, Type) = relateType[T](random)
    def relateType[T: TypeTag](lnf: => String): (IRI, Type) = {
        val tpe = typeTag[T].tpe
        val tid = relate(tpe.typeSymbol, TYPE, mmMapped, lnf)
        rtmap(ru.rootMirror.runtimeClass(tpe)) = tid
        (tid -> tpe)
    }

    def symOrNone(s: Symbol): Option[Symbol] = if (s != NoSymbol) Option(s) else None
    def add(s: Resource, p: IRI, o: Value, g: Option[Resource] = None) = mmodel.add(statement(s, p, o, g))
}

class Test1(name: String, age: Int) {
    var x: String = _
}

class Test2(on: Boolean) {
    def this(strFlag: String) = this(strFlag.toBoolean)
}

object testx {
    def main(args: Array[String]) {
        val b = new Test1("bob", 99)

        val mm = new Metamodel()
        mm.cascade[Test1]
        mm.cascade[Test2]

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
        println("1 " + mm.ctors(classOf[Test1]))
        println("2 " + mm.ctors(classOf[Test2]))



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

/*
caching may be able to be acheievd by query
 */
