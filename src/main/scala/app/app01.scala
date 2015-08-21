package app

import akka.actor.{Actor, ActorSystem, Props}
import akka.gpio.Conf.Directions.output
import akka.gpio.Conf._
import akka.gpio.Models
import com.pi4j.io.gpio.{PinState, RaspiPin}
import com.typesafe.scalalogging.LazyLogging
import pi.akka._


object app01 extends App {
    implicit val system = ActorSystem("app01")

    val pi = Pi(Models.bRev2)
    val config = gpio{ pin =>
        pin number 0 digital output
    }

    pi ! Configure(config)
    pi ! DigitalWrite(RaspiPin.GPIO_00, PinState.HIGH)

    val listener = system.actorOf(Props[Listener])
    listener ! Subscribe(RaspiPin.GPIO_01)
}

class Listener extends Actor with LazyLogging {
    def receive: Receive = {
        case DigitalEvent(p, v) => logger.info(s"observed digital event $v on $p")
    }
}
