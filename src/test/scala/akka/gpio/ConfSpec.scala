package akka.gpio

import akka.gpio.Conf.Directions.{input, output}
import akka.gpio.Conf._
import org.scalatest.{Matchers, WordSpecLike}

/**
 * @author wassj
 */
class ConfSpec extends WordSpecLike with Matchers {
    "config" should {
        "work" in {
            val conf = gpio { pin =>
                pin number 1 digital input
            }
            conf.pins().size shouldBe 1
        }
    }
}
