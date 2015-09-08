package rdf4s

import org.scalatest.Matchers
import rdf4s.AnnotatedSpecPkg.TypeAnnotated
import rdf4s.MetamodelQueries._

class AnnotatedSpec  extends MetamodelTest with Matchers {
    mmTest("ctorQuery") {
        install[TypeAnnotated]
        query(classOf[TypeAnnotated], ctors).get.size shouldBe 1
    }
}

package AnnotatedSpecPkg {

@useIRI("ns:t")
class TypeAnnotated(@useIRI("ns:p") p: String)

}
