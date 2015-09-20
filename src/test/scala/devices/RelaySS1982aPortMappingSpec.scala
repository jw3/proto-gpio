package devices

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import devices.RelaySS1982a.{In1, In2, In3, In4}
import gpio4s.PinAllocation
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpecLike}
import picfg.Directions.input
import picfg._

class RelaySS1982aPortMappingSpec extends TestKit(ActorSystem(getClass.getSimpleName.dropRight(1)))
                                          with ImplicitSender with WordSpecLike with Matchers with MockFactory {

    val conf = ConfigFactory.parseString(
        """
          |thermo.relay.ports.count = 4
          |thermo.relay.ports.1.pin = 5
          |thermo.relay.ports.2.pin = 6
          |thermo.relay.ports.3.pin = 7
          |thermo.relay.ports.4.pin = 8
        """.stripMargin)

    "mismatched port count" should {
    }

    "invalid port numbering" should {
    }

    "port to pin conversion" should {
        "map to appropriate ports" in {
            val expected = Map(In1 -> 5, In2 -> 6, In3 -> 7, In4 -> 8)
            RelaySS1982a.portToPin(conf.getConfig("thermo.relay")) shouldBe expected
        }
    }

    "info function" should {
        "generate appropriate configuration" in {
            val expected = gpio { pin =>
                pin number 5 digital input
                pin number 6 digital input
                pin number 7 digital input
                pin number 8 digital input
            }.withFallback(conf)

            RelaySS1982a.info("relay", conf).conf shouldBe expected
        }
    }

    "relay pin allocation handling" should {
        "map port to appropriate actor" in {
            val m1 = TestActorRef[MockActor]
            val m2 = TestActorRef[MockActor]
            val m3 = TestActorRef[MockActor]
            val m4 = TestActorRef[MockActor]
            val alloc: PinAllocation = Map(5 -> m1, 6 -> m2, 7 -> m3, 8 -> m4)

            val info = RelaySS1982a.info("relay", conf)
            val relay = TestActorRef[RelaySS1982a](Props(info.impl, info.id, info.conf, alloc)).underlyingActor

            relay.portsToGPIO(In1) shouldBe m1
            relay.portsToGPIO(In2) shouldBe m2
            relay.portsToGPIO(In3) shouldBe m3
            relay.portsToGPIO(In4) shouldBe m4
        }
    }
}


class MockActor extends Actor {
    def receive: Receive = {
        case _ =>
    }
}
