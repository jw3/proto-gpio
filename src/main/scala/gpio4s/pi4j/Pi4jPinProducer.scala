package gpio4s.pi4j

import akka.actor.{ActorContext, ActorRef}
import com.pi4j.io.gpio.impl.GpioPinImpl
import com.pi4j.io.gpio.{GpioController, GpioFactory, RaspiGpioProvider, RaspiPin}
import gpio4s.PinProducer

/**
 * Pi4j [[PinProducer]] implementation
 * @author  wassj
 */
class Pi4jPinProducer(c: GpioController, p: RaspiGpioProvider) extends PinProducer {
    def get(num: Int)(implicit context: ActorContext): ActorRef = {
        context.actorOf(Pi4jGpio.props(new GpioPinImpl(c, p, RaspiPin.getPinByName("GPIO " + num))))
    }
}

object Pi4jPinProducer {
    def apply(): Pi4jPinProducer = new Pi4jPinProducer(GpioFactory.getInstance, new RaspiGpioProvider)
}
