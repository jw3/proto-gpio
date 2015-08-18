package pi.akka

import akka.gpio.ModeEvent
import com.pi4j.io.gpio.{Pin, PinState}
import com.typesafe.config.Config


case class Configure(conf: Config)

case class Reset(pin: Pin) extends ModeEvent

private [akka] case class AsDigitalIn(pin: Pin) extends ModeEvent

private [akka] case class AsDigitalOut(pin: Pin) extends ModeEvent

case class DigitalEvent(pin: Pin, value: PinState)

case class DigitalWrite(pin: Pin, value: PinState)

case class DigitalRead(pin: Pin)

case class Subscribe(pin: Pin)

case class Unsubscribe(pin: Pin)
