package gpio4s

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{Matchers, WordSpecLike}

/**
 * @author wassj
 */
class GpioSetupSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with Matchers {
    def this() = this(ActorSystem(getClass.getSimpleName))

    "pi" must {
        "create all pins for model" in {

        }
    }
}
