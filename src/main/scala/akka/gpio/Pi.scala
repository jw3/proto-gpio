package pi.akka

import akka.actor._
import akka.gpio.PiModel
import com.pi4j.io.gpio._
import com.pi4j.io.gpio.impl.GpioPinImpl
import com.typesafe.config.Config

import scala.collection.mutable

object Pi {
    def apply(m: PiModel)(implicit sys: ActorSystem): ActorRef = apply(m, GpioFactory.getInstance, new RaspiGpioProvider)
    def apply(m: PiModel, c: GpioController, p: RaspiGpioProvider)(implicit sys: ActorSystem): ActorRef = sys.actorOf(props(m, c, p))
    def props(m: PiModel, c: GpioController, p: RaspiGpioProvider): Props = Props(new Pi(m, c, p))
}

class Pi(m: PiModel, c: GpioController, p: RaspiGpioProvider) extends Actor {
    val subscribers = new mutable.HashMap[Pin, mutable.Set[ActorRef]] with mutable.MultiMap[Pin, ActorRef]


    // init
    val gpio = m.pins.map { pin => pin -> context.actorOf(Gpio.props(new GpioPinImpl(c, p, pin))) }.toMap

    def configure(conf: Config): Unit = {
    }

    def receive: Receive = {
        case Configure(c) => configure(c)
        case e @ DigitalEvent(p, v) => subscribers(p).foreach(_ ! e)
        case DigitalWrite(p, v) => gpio(p) forward v
        case Subscribe(p) => subscribers.addBinding(p, sender())
        case Unsubscribe(p) => subscribers.removeBinding(p, sender())
        case _ =>
    }
}
