package pi.akka

import akka.gpio.api.ModeEvent
import com.pi4j.io.gpio.PinState


case class Reset() extends ModeEvent

case class AsDigitalIn() extends ModeEvent

case class AsDigitalOut() extends ModeEvent


case class DigitalEvent(state: PinState)

case class DigitalWrite(hi: Boolean)

case class DigitalRead()


case class Subscribe()

case class Unsubscribe()