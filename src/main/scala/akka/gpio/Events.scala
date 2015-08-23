package pi.akka

import akka.gpio.ModeEvent
import com.pi4j.io.gpio.{Pin, PinState}
import com.typesafe.config.Config


case class Configure(conf: Config)

case class Reset(pin: Int) extends ModeEvent

case class DigitalEvent(pin: Int, value: PinState)

case class DigitalWrite(pin: Int, value: PinState)

case class DigitalRead(pin: Int)

case class Subscribe(pin: Int)

case class Unsubscribe(pin: Int)


private [akka] case class AsDigitalIn() extends ModeEvent

private [akka] case class AsDigitalOut() extends ModeEvent

