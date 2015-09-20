package gpio4s

import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListener, GpioPinListenerDigital}

/**
 * @author wassj
 */
package object pi4j {

    implicit def DigitalListenerFunction(f: GpioPinDigitalStateChangeEvent => Unit): GpioPinListener = {
        new GpioPinListenerDigital {
            def handleGpioPinDigitalStateChangeEvent(e: GpioPinDigitalStateChangeEvent): Unit = f(e)
        }
    }
}
