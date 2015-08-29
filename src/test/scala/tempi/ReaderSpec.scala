package tempi

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpecLike}
import tempi.Reader.Subscribe
import tests._

/**
 * @author wassj
 */
class ReaderSpec extends TestKit(ActorSystem(getClass.getSimpleName.dropRight(1)))
                         with ImplicitSender with WordSpecLike with Matchers with MockFactory {

    "reader" must {
        "notify subscribers" in {
            val r = Reader()
            r ! Subscribe("id")
            r ! MockReading("id")
            expectMsg(MockReading("id"))
        }
    }
}

