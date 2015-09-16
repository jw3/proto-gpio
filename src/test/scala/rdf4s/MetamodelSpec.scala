package rdf4s

import org.scalatest.Matchers


class MetamodelSpec extends MetamodelTest with Matchers {
    mmTest("getter on public val field") {
        install[GetterOnPublicField]
        val res = query(classOf[GetterOnPublicField], MetamodelQueries.getters)
        res shouldBe defined
        res.get.size shouldBe 1
    }

    mmTest("no arg ctor") {
        install[EmptyCtor]
        val res = mm.query(classOf[EmptyCtor], MetamodelQueries.ctors)
        res shouldBe defined
        res.get.size shouldBe 1
        res.get.head._2 shouldBe empty
    }
}


class EmptyCtor()
class GetterOnPublicField {@useIRI("ns:f") val f = random()}
