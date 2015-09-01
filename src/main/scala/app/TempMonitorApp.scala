package app

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.pi4j.io.gpio.{GpioFactory, RaspiGpioProvider}
import com.typesafe.scalalogging.LazyLogging
import devices.RelaySS1982a
import devices.RelaySS1982a.In1
import gpio4s.Device.{DeviceInstallFailed, DeviceInstalled, DeviceMessage, InstallDevice}
import gpio4s.pi4j.Pi4jPinProducer
import gpio4s.{Configure, Models, Pi}
import picfg.PiCfg.Directions.output
import tempi.Reader.{Register, Subscribe}
import tempi.devices.DS18b20
import tempi.{Reader, Reading}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.io.Source

object TempMonitorApp {
    def main(args: Array[String]) {
        implicit val timeout = Timeout(10 seconds)
        implicit val system = ActorSystem("TempMonitorApp")

        val pi = Pi(Models.bRev2, new Pi4jPinProducer(GpioFactory.getInstance, new RaspiGpioProvider))
        pi ! Configure({ pin =>
            pin number 5 digital output
            pin number 6 digital output
        })

        val reader = Reader()

        val devId = "01"
        reader ! Register(devId, DS18b20(devId, Source.fromFile("/dev/w1/...")))
        reader.tell(Subscribe(devId), DashboardUpdater())

        val info = RelaySS1982a.info("relay", (11, 12, 13, 14))
        val response = pi ? InstallDevice(info)
        response.mapTo[DeviceMessage].onSuccess {
            case DeviceInstalled(id, ref) =>
                reader.tell(Subscribe(devId), RelayUpdater(ref))
            case DeviceInstallFailed(id, reason) =>
        }
    }

    class DashboardUpdater extends Actor with LazyLogging {
        def receive: Receive = {
            case Reading(id, c, f, t) =>
                logger.info(s"received temp update [${c}c/${f}f}] for dev[$id] at [$t]")
        }
    }
    object DashboardUpdater {
        def apply()(implicit sys: ActorSystem) = sys.actorOf(Props[DashboardUpdater])
    }

    class RelayUpdater(relay: ActorRef) extends Actor with LazyLogging {
        val desiredTemp = 50
        val lowerBounds = 10
        var burning = false

        def receive: Receive = {
            case Reading(id, c, f, t) =>
                logger.info(s"received temp update [${c}c/${f}f}] for dev[$id] at [$t]")
                if (!burning && c < desiredTemp - lowerBounds) {
                    burning = true
                    relay ! RelaySS1982a.Engage(In1)
                }
                else if (burning && c >= desiredTemp) {
                    relay ! RelaySS1982a.Release(In1)
                    burning = false
                }

        }
    }
    object RelayUpdater {
        def apply(relay: ActorRef)(implicit sys: ActorSystem) = sys.actorOf(Props(classOf[RelayUpdater], relay))
    }
}
