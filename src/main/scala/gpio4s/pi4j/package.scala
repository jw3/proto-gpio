package gpio4s

import akka.actor.{Actor, ActorContext, ActorRef, Props}
import com.pi4j.io.gpio._
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent
import com.pi4j.io.gpio.impl.GpioPinImpl
import picfg.PiCfg.PinDef

package object pi4j {
    /**
     * Actor interface to Pi4j GPIO
     * @author  wassj
     */
    class RaspiGpio(gpio: GpioPinImpl) extends Actor {
        def digitalIn: Receive = {
            case DigitalRead(_) => sender() ! gpio.isHigh
            case Reset(_) => reset()
        }

        def digitalOut: Receive = {
            case DigitalWrite(_, s) => gpio.setState(s)
            case Reset(_) => reset()
        }

        def receive: Receive = {
            case Setup(p) => setup(p)
        }

        def setup(pin: PinDef) = {
            pin.mode match {
                case digital if pin.dir.isInput => {
                    gpio.export(PinMode.DIGITAL_INPUT)
                    gpio.addListener(stateChangeListener)
                    context.become(digitalIn)
                }
                case digital if pin.dir.isOutput => {
                    gpio.export(PinMode.DIGITAL_OUTPUT)
                    context.become(digitalOut)
                }
            }
        }

        lazy val stateChangeListener = { e: GpioPinDigitalStateChangeEvent =>
            context.parent ! DigitalEvent(gpio.getPin.getAddress, e.getState.isHigh)
        }

        def reset(): Unit = {
            gpio.removeListener(stateChangeListener)
            context.become(receive)
        }
    }

    object RaspiGpio {
        def props(gpio: GpioPinImpl): Props = Props(new RaspiGpio(gpio))
    }

    /**
     * Pi4j [[PinProducer]] implementation
     * @author  wassj
     */
    class Pi4jPinProducer(c: GpioController, p: RaspiGpioProvider) extends PinProducer {
        def get(num: Int)(implicit context: ActorContext): ActorRef = {
            context.actorOf(RaspiGpio.props(new GpioPinImpl(c, p, RaspiPin.getPinByName("GPIO " + num))))
        }
    }
}
