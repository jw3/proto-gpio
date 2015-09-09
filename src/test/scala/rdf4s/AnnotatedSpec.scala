package rdf4s

import org.scalatest.Matchers
import rdf4s.MetamodelQueries._

import scala.annotation.StaticAnnotation


class AnnotatedSpec extends MetamodelTest with Matchers {
    import AnnotatedSpec._

    mmTest("literal") {
        install[TypeAnnotated]
        query(classOf[TypeAnnotated], ctors).get.size shouldBe 1
    }
    mmTest("string") {
        install(classOf[TypeAnnotated].getName)
        query(classOf[TypeAnnotated], ctors).get.size shouldBe 1
    }
}

object AnnotatedSpec {
    @useIRI("ns:scalaAnno") case class ScalaAnnotation(s:String) extends StaticAnnotation

    @ScalaAnnotation("foo")
    @useIRI("ns:t")
    class TypeAnnotated(@useIRI("ns:p") p: String)
}
