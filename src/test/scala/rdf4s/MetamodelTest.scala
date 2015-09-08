package rdf4s

import org.openrdf.model.{Model, Resource}
import org.scalatest.WordSpec
import rdf4s.Metamodel.LookupFn

import scala.reflect.ClassTag

trait MetamodelTest extends WordSpec {
    private var _mm: Metamodel = _
    private def mm() = _mm

    def query[R](t: Class[_], q: (Resource, Model, LookupFn) => R): Option[R] = mm.query(t, q)

    def install(fqcn: String) = mm.install(fqcn)
    def install[T: ClassTag] = mm.install[T]

    def mmTest(name: String)(testFunction: => Unit) = registerTest(name) {
        _mm = new Metamodel
        testFunction
        _mm = null
    }
}
