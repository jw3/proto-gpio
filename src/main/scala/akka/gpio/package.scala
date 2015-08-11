package akka

import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListener, GpioPinListenerDigital}
import com.pi4j.io.gpio.{GpioPinDigitalInput, GpioPinDigitalOutput}

package object gpio {

  implicit def DigitalListenerFunction(f: GpioPinDigitalStateChangeEvent => Unit): GpioPinListener =
    new GpioPinListenerDigital {
      def handleGpioPinDigitalStateChangeEvent(e: GpioPinDigitalStateChangeEvent): Unit = f(e)
    }

  implicit def DigitalInput(impl: GpioPinDigitalInput): GpioDigitalInput = new GpioDigitalInput {
  }

  implicit def DigitalOutput(impl: GpioPinDigitalOutput): GpioDigitalOutput = new GpioDigitalOutput {
  }

  trait Device

  trait GpioDigitalInput

  trait GpioDigitalOutput
}
