package app

import akka.actor._
import akka.gpio.Conf.Directions.output
import akka.gpio.Conf._
import akka.gpio.Models
import com.pi4j.io.gpio.impl.GpioPinImpl
import com.pi4j.io.gpio._
import com.typesafe.scalalogging.LazyLogging
import pi.akka._

object app01 extends App {
    implicit val system = ActorSystem("app01")

    val pi = Pi(Models.bRev2, new Pi4jPinProducer(GpioFactory.getInstance, new RaspiGpioProvider))
    val config = gpio { pin =>
        pin number 0 digital output
    }

    pi ! Configure(config)
    pi ! DigitalWrite(0, PinState.HIGH)

    val listener = system.actorOf(Props[Listener])
    listener ! Subscribe(1)
}

class Listener extends Actor with LazyLogging {
    def receive: Receive = {
        case DigitalEvent(p, v) => logger.info(s"observed digital event $v on $p")
    }
}

class Pi4jPinProducer(c: GpioController, p: RaspiGpioProvider) extends PinProducer {
    def get(num: Int)(implicit context: ActorContext): ActorRef = {
        context.actorOf(Gpio.props(new GpioPinImpl(c, p, RaspiPin.getPinByName("GPIO " + num))))
    }
}
