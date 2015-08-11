package pi.akka

import akka.actor.{Actor, ActorRef, Props}
import akka.gpio._
import com.pi4j.io.gpio.PinMode
import com.pi4j.io.gpio.event._
import com.pi4j.io.gpio.impl.GpioPinImpl

import scala.collection.mutable

object Gpio {
  def props(gpio: GpioPinImpl): Props = Props(new Gpio(gpio))
}

class Gpio(val gpio: GpioPinImpl) extends Actor {
  val subscribers: collection.mutable.Set[ActorRef] = new mutable.HashSet[ActorRef]

  def digitalIn: Receive = {
    case DigitalRead() => sender() ! gpio.isHigh
    case Reset() => reset()
  }

  def digitalOut: Receive = {
    case DigitalWrite(v) => gpio.setState(v)
    case Reset() => reset()
  }

  def receive: Receive = {
    case AsDigitalIn() => {
      gpio.export(PinMode.DIGITAL_INPUT)
      gpio.addListener { e: GpioPinDigitalStateChangeEvent => context.parent ! DigitalEvent(e.getState) }
      context.become(digitalIn)
    }
    case AsDigitalOut() => {
      gpio.export(PinMode.DIGITAL_OUTPUT)
      gpio.removeAllListeners()
      context.become(digitalOut)
    }
    case Subscribe() => subscribe(sender())
  }

  def subscribe(actor: ActorRef): Unit = subscribers.add(actor)

  def unsubscribe(actor: ActorRef): Unit = subscribers.remove(actor)

  def reset(): Unit = context.become(receive)
}
