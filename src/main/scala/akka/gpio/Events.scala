package pi.akka

import akka.gpio.ModeEvent
import com.pi4j.io.gpio.{Pin, PinState}


case class Reset(pin: Pin) extends ModeEvent

case class AsDigitalIn(pin: Pin) extends ModeEvent

case class AsDigitalOut(pin: Pin) extends ModeEvent

case class DigitalEvent(pin: Pin, value: PinState)

case class DigitalWrite(pin: Pin, value: PinState)

case class DigitalRead(pin: Pin)

case class Subscribe(pin: Pin)

case class Unsubscribe(pin: Pin)

case class Status()
