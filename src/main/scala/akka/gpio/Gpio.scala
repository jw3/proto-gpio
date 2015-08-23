package pi.akka

import akka.actor.{Actor, Props}
import akka.gpio._
import com.pi4j.io.gpio.PinMode
import com.pi4j.io.gpio.event._
import com.pi4j.io.gpio.impl.GpioPinImpl

object Gpio {
    def props(gpio: GpioPinImpl): Props = Props(new Gpio(gpio))
}

class Gpio(val gpio: GpioPinImpl) extends Actor {
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
