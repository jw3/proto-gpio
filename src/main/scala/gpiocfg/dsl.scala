package gpiocfg

import com.typesafe.config.Config
import gpiocfg.dsl.{AnalogInitializer, DigitalInitializer, PinModeBuilder, PinNumberBuilder}

/**
 * DSL describing the configuration of GPIO pins
 *
 * usage:
 *
 * gpio { pin =>
 * pin number 1 digital input
 * pin nuber 2 analog output
 * }
 *
 *
 *
 *
 *
 * @author wassj
 */
object dsl {

    /**
     * entrypoint to DSL
     * @param fn Function that configures the builder
     * @return
     */
    def gpio(fn: PinNumberBuilder => Unit): Config = {
        val b = new PinBuilder
        fn(b)
        b.build
    }


    /**
     * describes a gpio numbering scheme
     * todo;; this will be opened up to be client extensible
     */
    sealed trait Layout extends DslComponent
    case object bcom extends Layout
    case object pi4j extends Layout


    /**
     * describes the mode of a pin (digital/analog)
     */
    sealed trait Mode extends DslComponent
    object Modes {
        case object digital extends Mode
        case object analog extends Mode
        case object pwm extends Mode
    }


    /**
     * describes the direction (input/output) of a pin
     */
    sealed trait Direction extends DslComponent {
        def isInput = this == input
        def isOutput = this == output
    }
    case object input extends Direction
    case object output extends Direction


    /**
     * describes the internal resistor state (pull up/down or off)
     */
    sealed trait Pull extends DslComponent
    case object off extends Pull
    case object up extends Pull
    case object down extends Pull


    /**
     * describes the digital state (hi or low) of a pin
     */
    sealed trait DigitalState extends DslComponent
    case object hi extends DigitalState
    case object low extends DigitalState


    trait PinNumberBuilder {
        def number(num: Int): PinModeBuilder
    }

    trait PinModeBuilder {
        def digital(d: Direction): DigitalInitializer
        def analog(d: Direction): AnalogInitializer
        def pwm(): Unit
    }

    trait Initializer {
        def pull(dir: Pull)
    }

    trait DigitalInitializer extends Initializer {
        def set(v: DigitalState): Initializer
    }

    trait AnalogInitializer extends Initializer {
        def value(v: Double): Initializer
    }
}

// internal API
sealed trait DslComponent extends Product {
    this: Product =>
    val uid = productPrefix
}

// default implementation of the dsl
private class PinBuilder extends PinNumberBuilder with PinModeBuilder with DigitalInitializer with AnalogInitializer {
    import com.typesafe.config.{ConfigFactory => cf, ConfigValueFactory => cvf}
    import gpiocfg.dsl._

    import scala.collection.JavaConversions._

    var num: Int = -1
    var layout: Layout = pi4j
    val pins = collection.mutable.Set[Int]()
    val modes = collection.mutable.Map[Int, Mode]()
    val directions = collection.mutable.Map[Int, Direction]()
    val values = collection.mutable.Map[Int, AnyRef]()
    val pulls = collection.mutable.Map[Int, Pull]()

    def value(v: Double): Initializer = {
        values(num) = Double.box(v)
        this
    }

    def pull(dir: Pull): Unit = pulls(num) = dir

    def number(num: Int) = {
        // bounds check valid pin 0-?
        this.num = num
        pins.add(num)
        this
    }
    def digital(d: Direction): DigitalInitializer = {
        modes(num) = Modes.digital
        directions(num) = d
        this
    }
    def analog(d: Direction): AnalogInitializer = {
        modes(num) = Modes.analog
        directions(num) = d
        this
    }
    def pwm(): Unit = {
        modes(num) = Modes.pwm
        directions(num) = output
    }

    def set(v: DigitalState): Initializer = {
        values(num) = v
        this
    }

    def build: Config = {
        num = -1

        val map = collection.mutable.Map[String, AnyRef]()
        map("version") = Int.box(1)
        map("layout") = layout.uid

        val pinout = for (pin <- pins) yield {
            val pinmap = collection.mutable.Map[String, AnyRef]()
            pinmap("number") = Int.box(pin)
            pinmap("mode") = modes(pin).uid
            pinmap("direction") = directions(pin).uid
            values.get(pin) foreach {
                case v: java.lang.Double => pinmap("set") = Double.box(v)
                case v: DigitalState => pinmap("set") = v.uid
            }
            pulls.get(pin).foreach { v => pinmap("pull") = v.uid }
            cvf.fromMap(pinmap)
        }

        map("pins") = cvf.fromIterable(pinout)
        cf.parseMap(map)
    }
}
