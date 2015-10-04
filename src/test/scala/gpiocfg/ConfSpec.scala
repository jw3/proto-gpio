package gpiocfg

import org.scalatest.{Matchers, WordSpecLike}
import gpiocfg.dsl.Modes.digital
import gpiocfg.dsl._
import gpiocfg.io._


/**
 * @author wassj
 */
class ConfSpec extends WordSpecLike with Matchers {
    "bean access" should {
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
