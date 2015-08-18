package akka.gpio

import akka.gpio.dsltest.Directions.{Direction, input, output}
import akka.gpio.dsltest.Layouts.Layout
import akka.gpio.dsltest.Modes.Mode
import com.typesafe.config.{Config, ConfigFactory => cf, ConfigValueFactory => cvf}

import scala.collection.JavaConversions._

/**
 * Created by wassj on 8/16/2015.
 */
object dsltest {
    //    trait S
    //    trait P
    //    trait O
    //    type R = _
    //
    //    def article: sbuilder = _
    //
    //    trait sbuilder {
    //        def s(s: S): pbuilder = _
    //    }
    //    trait pbuilder {
    //        def p(p: P): obuilder = _
    //    }
    //    trait obuilder {
    //        def o(o: O): R = _
    //    }
    //
    //    article s (s) p (p) o (o)

    /**
     * article -> subject -> predicate -> value
     *
     * article provides the builder
     * subject defines the target
     * predicate defines the property
     * value defines the property's value
     */

    //    def s: S = new S {}
    //    def p: P = new P {}
    //    def o: O = new O {}

    // pin number 1 is digital input
    // pin number 2 is digital output
    // pin number 3 is pwm

    object Layouts {
        trait Layout extends Product
        case object pi4j extends Layout
        case object bcom extends Layout
    }
    object Modes {
        trait Mode extends Product
        case object digital extends Mode
        case object analog extends Mode
        case object pwm extends Mode
    }
    object Directions {
        trait Direction extends Product
        case object input extends Direction
        case object output extends Direction
    }
    trait PinNumberBuilder {
        def number(num: Int): PinModeBuilder
    }
    trait PinModeBuilder {
        def digital(d: Direction)
        def analog(d: Direction)
        def pwm
    }
    trait PinDirectionBuilder {
    }

    class PinBuilder extends PinNumberBuilder with PinModeBuilder {
        var num: Int = -1
        val pins = collection.mutable.Set[Int]()
        val modes = collection.mutable.Map[Int, Mode]()
        val directions = collection.mutable.Map[Int, Direction]()
        var layout: Layout = Layouts.pi4j

        def number(num: Int) = {
            this.num = num
            pins.add(num)
            this
        }
        def digital(d: Direction) = {
            modes(num) = Modes.digital
            directions(num) = d
        }
        def analog(d: Direction) = {
            modes(num) = Modes.analog
            directions(num) = d
        }
        def pwm = {
            modes(num) = Modes.pwm
            directions(num) = Directions.output
        }

        def build: Config = {
            val map = collection.mutable.Map[String, AnyRef]()
            map("version") = Int.box(1)
            map("layout") = layout.productPrefix

            val pinout = for (pin <- pins) yield {
                val pinmap = collection.mutable.Map[String, AnyRef]()
                pinmap("number") = Int.box(pin)
                pinmap("mode") = modes(pin).productPrefix
                pinmap("direction") = directions(pin).productPrefix
                cvf.fromMap(pinmap)
            }

            map("pins") = cvf.fromIterable(pinout)
            cf.parseMap(map)
        }
    }

    class PinDef {
        var number: Int = _
        var direction: Direction = _
        var mode: Mode = _
    }


    def gpio(fn: PinBuilder => Unit): Config = {
        val b = new PinBuilder
        fn(b)
        b.build
    }


    def main(args: Array[String]) {
        val conf = gpio { pin =>
            pin number 1 digital input
            pin number 2 analog output
            pin number 22 pwm
        }
        println(conf)
    }
}
