package rdf4s

import org.openrdf.model.{Model, Resource}
import org.scalatest.WordSpec
import rdf4s.Metamodel.{QueryFn, LookupFn}

import scala.collection.JavaConversions._
import scala.reflect.ClassTag

trait MetamodelTest extends WordSpec {
    private implicit var _mm: Metamodel = _
    implicit def mm: Metamodel = _mm

    def print = mm.mmodel.foreach(println)
    def query[R](t: Class[_], q: QueryFn[R]): Option[R] = mm.query(t, q)

    def install(fqcn: String) = mm.install(fqcn)
    def install[T: ClassTag] = mm.install[T]

    def mmTest(name: String)(testFunction: => Unit) = registerTest(name) {
        _mm = new Metamodel
        testFunction
        _mm = null
    }
}
