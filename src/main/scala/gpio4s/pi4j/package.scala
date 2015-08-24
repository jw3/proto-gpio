package gpio4s

import akka.actor.{ActorRef, ActorContext, Actor, Props}
import com.pi4j.io.gpio.{RaspiPin, RaspiGpioProvider, GpioController, PinMode}
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent
import com.pi4j.io.gpio.impl.GpioPinImpl

package object pi4j {
    object RaspiGpio {
        def props(gpio: GpioPinImpl): Props = Props(new RaspiGpio(gpio))
    }

    class RaspiGpio(val gpio: GpioPinImpl) extends Actor {
        def digitalIn: Receive = {
            case DigitalRead(_) => sender() ! gpio.isHigh
            case Reset(_) => reset()
        }

        def digitalOut: Receive = {
            case DigitalWrite(_, s) => gpio.setState(s)
            case Reset(_) => reset()
        }

        def receive: Receive = {
            case AsDigitalIn() => {
                gpio.export(PinMode.DIGITAL_INPUT)
                gpio.addListener(stateChangeListener)
                context.become(digitalIn)
            }
            case AsDigitalOut() => {
                gpio.export(PinMode.DIGITAL_OUTPUT)
                context.become(digitalOut)
            }
        }

        lazy val stateChangeListener = { e: GpioPinDigitalStateChangeEvent =>
            context.parent ! DigitalEvent(gpio.getPin.getAddress, e.getState)
        }

        def reset(): Unit = {
            gpio.removeListener(stateChangeListener)
            context.become(receive)
        }
    }

    class Pi4jPinProducer(c: GpioController, p: RaspiGpioProvider) extends PinProducer {
        def get(num: Int)(implicit context: ActorContext): ActorRef = {
            context.actorOf(RaspiGpio.props(new GpioPinImpl(c, p, RaspiPin.getPinByName("GPIO " + num))))
        }
    }
}
