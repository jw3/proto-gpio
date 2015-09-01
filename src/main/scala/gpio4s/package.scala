import akka.actor.{Actor, ActorContext, ActorRef, Props}
import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListener, GpioPinListenerDigital}
import com.typesafe.config.Config
import picfg.PiCfg
import picfg.PiCfg.PinDef


package object gpio4s {
    type PinAllocation = Map[Int, ActorRef]

    trait PinProducer {
        def get(num: Int)(implicit context: ActorContext): ActorRef
    }

    trait DeviceInfo {
        def id: String
        def conf: Config
        def impl: Class[_ <: Device]

        final def props(pins: PinAllocation): Props = Props(impl, id, conf, pins)
    }
    /**
     * Tagging interface that identifies a Device Actor
     * A Device requires a ctor that takes
     */
    trait Device extends Actor
    object Device {
        sealed trait DeviceMessage
        case class InstallDevice(info: DeviceInfo) extends DeviceMessage
        case class DeviceInstalled(id: String, actorRef: ActorRef) extends DeviceMessage
        case class DeviceInstallFailed(id: String, e: Throwable) extends DeviceMessage
    }

    trait ModeEvent

    // pi events
    case class Configure(conf: Config)
    object Configure {
        import PiCfg._
        def apply(fn: PinNumberBuilder => Unit): Configure = Configure(gpio(fn))
    }
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
