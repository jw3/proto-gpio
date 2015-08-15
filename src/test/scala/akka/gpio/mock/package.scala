/*
package akka.gpio

import java.util.UUID

import com.pi4j.io.gpio._
import com.pi4j.io.gpio.impl.GpioControllerImpl

package object mock {

    lazy val mockController = new GpioControllerImpl(new MockGpioProvider)
    val x = new gpio {
        set(RaspiPin.GPIO_01)
    }
    val in = new Mode() {}


    /// dsl

    // pin as input with value(1)

    // pin# analog input value(1)

    // pin mode
    // 1 as analog input with value(1)
    val out = new Mode() {}

    def input = new Mode() {}

    def pin(p: Pin) = ()

    def controller(pfn: Provisioner => Unit): GpioController = {
        val defaultProvisioner = new DefaultProvisioner
        pfn(defaultProvisioner)
        null
    }

    private def unique = UUID.randomUUID().toString.substring(0, 7)

    trait gpio {
        def set(p: Pin): B = B(p)
    }

    trait Provisioner {
        def analog(mode: Mode)

        def digital(mode: Mode)
    }

    trait Mode {
        def from(p: Pin): Mode = {
            this
        }
    }

    trait AnalogSetter {
        def setTo(value: Double)
    }

    class MockGpioProvider extends GpioProviderBase {
        override def getName: String = "MockGpioProvider" + unique
    }

    case class B(p: Pin) {

    }

    class DefaultProvisioner extends Provisioner {
        override def analog(pin: Pin): AnalogSetter = {
            new DefaultAnalogSetter
        }
    }

    class DefaultAnalogSetter extends AnalogSetter {
        override def setTo(v: Double) = ()
    }

}
*/
