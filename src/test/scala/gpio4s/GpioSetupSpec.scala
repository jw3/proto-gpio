package gpio4s

import akka.actor.{ActorContext, ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import gpio4s.PiModels.bRev2
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpecLike}
import gpiocfg.dsl._
import gpiocfg.io.RichPins


/**
 * @author wassj
 */
class GpioSetupSpec extends TestKit(ActorSystem(getClass.getSimpleName.dropRight(1)))
                            with ImplicitSender with WordSpecLike with Matchers with MockFactory {

    "pi" must {
        "create all pins for model" in {
            val producer = stub[PinProducer]
            (producer.get(_: Int)(_: ActorContext)).when(*, *).returning(testActor).repeat(bRev2.pins.indices)
            Pi(bRev2, producer)
            Thread.sleep(1000)
        }

        "configure pins" in {
            val probe = TestProbe()
            val pi = Pi(bRev2, probedProducer(probe))

            val conf = gpio {_ number 0 digital input}
            pi ! Configure(conf)

            probe.expectMsg(Setup(conf.pins().head))
        }

        "relay events" in {
            val pi = Pi(bRev2, producer)

            val subs = TestProbe()
            subs.send(pi, Subscribe(0))

            val e = DigitalEvent(0, true)
            pi ! e
            subs.expectMsg(e)
        }
    }


    def producer: PinProducer = new PinProducer {
        def get(num: Int)(implicit context: ActorContext): ActorRef = testActor
    }

    def probedProducer(probe: TestProbe): PinProducer = new PinProducer {
        def get(num: Int)(implicit context: ActorContext): ActorRef = probe.ref
    }
}
