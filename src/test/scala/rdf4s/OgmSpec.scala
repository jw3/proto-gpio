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
        ogm.write(new SingleField).map(_._2.foreach(println))
    }

    mmTest("single field as ctor param") {
        install[SingleCtorField]
        ogm.write(new SingleCtorField("b")).map(_._2.foreach(println))
    }

    mmTest("all val types") {
        install[AllValTypes]
        ogm.write(new AllValTypes(0, 1, 2, 3, 4, 5)).map(_._2.foreach(println))
    }

    mmTest("nested mapped type") {
        install[HasNested]
        install[IsNested]
        ogm.write(new HasNested(new IsNested)).map(_._2.foreach(println))
    }

    mmTest("read single field object") {
        install[SingleCtorField]

        val obj = new SingleCtorField("test")
        val result = ogm.write(obj).map { r =>
            r._2.addAll(mm.mmodel)
            r
        }.flatMap(r => ogm.read[SingleCtorField](r._1, r._2))

        result shouldBe defined
        result.get.f shouldBe obj.f
    }
}

class SingleField {val f = "a"}
@useIRI("ns:TheClass") class SingleCtorField @useIRI("ns:TheCTOR")(@useIRI("ns:TheProperty") val f: String)
class AllValTypes(val b: Byte, val s: Short, val i: Int, val l: Long, val f: Float, val d: Double)

class HasNested(xn: IsNested) {
    val n: IsNested = xn
}

class IsNested
