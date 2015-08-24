import com.pi4j.io.gpio.{GpioPinDigitalOutput, GpioPinDigitalInput, PinState}
import com.pi4j.io.gpio.event.{GpioPinListenerDigital, GpioPinListener, GpioPinDigitalStateChangeEvent}
import com.typesafe.config.Config


package object gpio4s {
    trait ModeEvent

    case class Configure(conf: Config)

    case class DigitalEvent(pin: Int, value: PinState)

    case class DigitalWrite(pin: Int, value: PinState)

    case class DigitalRead(pin: Int)

    case class Subscribe(pin: Int)

    case class Unsubscribe(pin: Int)


    case class Reset(pin: Int) extends ModeEvent

    private [gpio4s] case class AsDigitalIn() extends ModeEvent

    private [gpio4s] case class AsDigitalOut() extends ModeEvent

    ////

    implicit def DigitalListenerFunction(f: GpioPinDigitalStateChangeEvent => Unit): GpioPinListener = {
        new GpioPinListenerDigital {
            def handleGpioPinDigitalStateChangeEvent(e: GpioPinDigitalStateChangeEvent): Unit = f(e)
        }
    }
}
