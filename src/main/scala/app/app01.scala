package app

import akka.actor._
import com.pi4j.io.gpio._
import com.typesafe.scalalogging.LazyLogging
import gpio4s._
import gpio4s.pi4j.Pi4jPinProducer
import picfg.PiCfg.Directions.output
import picfg.PiCfg._

object app01 extends App {
    implicit val system = ActorSystem("app01")

    val pi = Pi(Models.bRev2, new Pi4jPinProducer(GpioFactory.getInstance, new RaspiGpioProvider))
    val config = gpio { pin =>
        pin number 0 digital output
    }

    pi ! Configure(config)
    pi ! DigitalWrite(0, true)

    val listener = system.actorOf(Props[Listener])
    listener ! Subscribe(1)
}

class Listener extends Actor with LazyLogging {
    def receive: Receive = {
        case DigitalEvent(p, v) => logger.info(s"observed digital event $v on $p")
    }
}


