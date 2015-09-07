package rdf4s

import org.scalatest.Matchers
import rdf4s.MetamodelQueries._
import rdf4s.MetamodelSpecPkg._


/**
 * @author wassj
 */
class MetamodelSpec extends MetamodelTest with Matchers {

    mmTest("ctorQuery") {
        install[One]
        query(classOf[One], ctors).get.size shouldBe 1
    }
}

package MetamodelSpecPkg {

class One
class Two {
    def this(v: Int) = this()
}

}
