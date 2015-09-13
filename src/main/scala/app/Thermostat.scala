package app

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import devices.RelaySS1982a
import devices.RelaySS1982a.In1
import gpio4s.Device.{DeviceInstallFailed, DeviceInstalled, DeviceMessage, InstallDevice}
import gpio4s.pi4j._
import gpio4s.{Configure, Models, Pi}
import picfg.PiCfg.Directions.output
import tempi.Reader.{Register, Subscribe}
import tempi.devices.DS18b20
import tempi.{Reader, Reading}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

/**
 * Model a Thermostat.  There is a single temperature device attached
 * and a threshold assigned.  When the threshold is exceeded an attached
 * relay is engaged until the threshold is within range again.
 */
object Thermostat extends LazyLogging {
    def main(args: Array[String]) {
        implicit val timeout = Timeout(10 seconds)
        implicit val system = ActorSystem("TempMonitorApp")
        val config = ConfigFactory.load("tempi.conf")

        // create a new Pi
        val pi = Pi(Models.bRev2, pi4jPins())

        // configure the pi with the dsl
        pi ! Configure({ pin =>
            pin number 5 digital output
            pin number 6 digital output
        })

        // create the temp reader
        val reader = Reader()

        // create a temperature device, and hook it up to the dashboard
        val devId = config.getString("tempi.device")
        reader ! Register(devId, DS18b20(devId))

        // install a SainSmart 4channel relay on the pi
        val info = RelaySS1982a.info("relay", (11, 12, 13, 14))
        val response = pi ? InstallDevice(info)
        response.mapTo[DeviceMessage].onSuccess {
            case DeviceInstalled(id, ref) =>
                reader.tell(Subscribe(devId), RelayUpdater(ref))
            case DeviceInstallFailed(id, reason) =>
                logger.error(s"device $id install failed", reason)
        }
    }

    /**
     * actor that converts readings into UI updates
     */
    class DashboardUpdater extends Actor with LazyLogging {
        def receive: Receive = {
            case Reading(id, c, f, t) =>
                logger.info(s"received temp update [${c}c/${f}f}] for dev[$id] at [$t]")
        }
    }
    object DashboardUpdater {
        def apply()(implicit sys: ActorSystem) = sys.actorOf(Props[DashboardUpdater])
    }

    /**
     * actor that turns readings into physical changes through the attached relay
     */
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
