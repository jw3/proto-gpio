package picfg

import org.scalatest.{Matchers, WordSpecLike}
import picfg.DigitalState.hi
import picfg.Directions.output
import picfg.DigitalState.hi
import picfg.Modes.digital
import picfg.Pulls.down
import picfg._

/**
 * @author wassj
 */
class ConfSpec extends WordSpecLike with Matchers {
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
