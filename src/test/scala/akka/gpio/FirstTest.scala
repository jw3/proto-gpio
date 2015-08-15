package akka.gpio

import akka.actor.ActorSystem
import akka.testkit.{TestKit, ImplicitSender}
import org.scalatest.{WordSpecLike, Matchers}

class FirstTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with Matchers {
    def this() = this(ActorSystem("FirstTest"))

    "pi" must {
        "create all pins for model" in {

        }
    }
}
