package app

import akka.actor.{Actor, ActorSystem, Props}
import com.pi4j.io.gpio.{GpioFactory, RaspiGpioProvider}
import com.typesafe.scalalogging.LazyLogging
import gpio4s.pi4j.Pi4jPinProducer
import gpio4s.{Configure, Models, Pi}
import picfg.PiCfg.Directions.output
import tempi.Reader.{Register, Subscribe}
import tempi.devices.DS18b20
import tempi.{Reader, Reading}

import scala.io.Source

object TempMonitorApp {
    def main(args: Array[String]) {
        implicit val system = ActorSystem("TempMonitorApp")

        val pi = Pi(Models.bRev2, new Pi4jPinProducer(GpioFactory.getInstance, new RaspiGpioProvider))
        pi ! Configure({ pin =>
            pin number 13 digital output
            pin number 14 digital output
        })

        val reader = Reader()

        val devId = "01"
        reader ! Register(devId, DS18b20(devId, Source.fromFile("/dev/w1/...")))
        reader.tell(Subscribe(devId), DashboardUpdater())
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
}
