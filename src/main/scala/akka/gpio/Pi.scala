package pi.akka

import akka.actor.{Actor, Props}
import akka.gpio.api.PiModel
import com.pi4j.io.gpio._
import com.pi4j.io.gpio.impl.GpioPinImpl

object Pi {
  def props(model: PiModel): Props = Props(new Pi(model))
}

class Pi(val model: PiModel) extends Actor {
  val controller: GpioController = GpioFactory.getInstance()
  val provider: GpioProvider = new RaspiGpioProvider

  model.pins.foreach(pin => context.actorOf(Gpio.props(new GpioPinImpl(controller, provider, pin))))

  def receive: Receive = {
    case DigitalEvent(state) => {

    }
    case _ =>
  }
}
