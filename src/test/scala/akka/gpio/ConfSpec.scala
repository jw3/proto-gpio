package akka.gpio

import akka.gpio.Conf.DigitalState.{hi, low}
import akka.gpio.Conf.Directions.{input, output}
import akka.gpio.Conf.Modes.digital
import akka.gpio.Conf.Pulls.{down, up}
import akka.gpio.Conf._
import com.typesafe.config.ConfigRenderOptions
import org.scalatest.{Matchers, WordSpecLike}

/**
 * @author wassj
 */
class ConfSpec extends WordSpecLike with Matchers {
    "config" should {
        "work" in {
            val conf = gpio { pin =>
                pin number 0 digital output set low pull up
                pin number 1 digital input pull down
                pin number 2 digital output
            }
            println(conf.root().render(ConfigRenderOptions.defaults().setOriginComments(false)))
            conf.pins().size shouldBe 3
        }
    }

    "digital configurations" should {
        "basic" in {
            val conf = gpio {_ number 0 digital output}
            val pin = conf.pins.head
            pin.num shouldBe 0
            pin.mode shouldBe digital
            pin.dir shouldBe output
            pin.state shouldBe None
            pin.pull shouldBe None
        }

        "with state" in {
            val conf = gpio {_ number 0 digital output set hi}
            val pin = conf.pins.head
            pin.num shouldBe 0
            pin.mode shouldBe digital
            pin.dir shouldBe output
            pin.state shouldBe Some(hi)
            pin.pull shouldBe None
        }

        "with pull" in {
            val conf = gpio {_ number 0 digital output set hi pull down}
            val pin = conf.pins.head
            pin.num shouldBe 0
            pin.mode shouldBe digital
            pin.dir shouldBe output
            pin.state shouldBe Some(hi)
            pin.pull shouldBe Some(down)
        }
    }
}
