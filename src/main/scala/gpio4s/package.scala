import akka.actor.{ActorContext, ActorRef}
import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListener, GpioPinListenerDigital}
import com.typesafe.config.Config
import picfg.PiCfg.PinDef


package object gpio4s {
    trait PinProducer {
        def get(num: Int)(implicit context: ActorContext): ActorRef
    }

    trait ModeEvent

    // pi events
    case class Configure(conf: Config)
    case class Subscribe(pin: Int)
    case class Unsubscribe(pin: Int)

    // pin events
    case class DigitalWrite(pin: Int, state: Boolean)
    case class DigitalRead(pin: Int)

    private[gpio4s] case class Reset(pin: Int) extends ModeEvent
    private[gpio4s] case class Setup(pin: PinDef) extends ModeEvent

    // responses
    case class DigitalEvent(pin: Int, state: Boolean)

    ////

    implicit def DigitalListenerFunction(f: GpioPinDigitalStateChangeEvent => Unit): GpioPinListener = {
        new GpioPinListenerDigital {
            def handleGpioPinDigitalStateChangeEvent(e: GpioPinDigitalStateChangeEvent): Unit = f(e)
        }
    }
}
