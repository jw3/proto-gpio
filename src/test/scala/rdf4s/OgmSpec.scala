package rdf4s

import org.scalatest.Matchers
import rdf4s.implicits._

import scala.collection.JavaConversions._

/**
 * @author wassj
 */
class OgmSpec extends MetamodelTest with Matchers {

    mmTest("single field") {
        install[SingleField]
        ogm.write(new SingleField).map(_.foreach(println))
    }

    mmTest("single field as ctor param") {
        install[SinbleCtorField]
        ogm.write(new SinbleCtorField("b")).map(_.foreach(println))
    }

    mmTest("all val types") {
        install[AllValTypes]
        ogm.write(new AllValTypes(0, 1, 2, 3, 4, 5)).map(_.foreach(println))
    }

    mmTest("nested mapped type") {
        install[HasNested]
        install[IsNested]
        ogm.write(new HasNested(new IsNested)).map(_.foreach(println))
    }
}

class SingleField {val f = "a"}
class SinbleCtorField(val f: String)
class AllValTypes(val b: Byte, val s: Short, val i: Int, val l: Long, val f: Float, val d: Double)

class HasNested(n: IsNested)
class IsNested
