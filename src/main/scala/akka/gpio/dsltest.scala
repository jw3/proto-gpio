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
        sealed trait Layout {
            this: Product =>
            val uid = productPrefix
        }
        case object pi4j extends Layout
        case object bcom extends Layout

        def from(str: String): Option[Layout] = str match {
            case pi4j.uid => Option(pi4j)
            case bcom.uid => Option(bcom)
            case _ => None
        }
    }
    object Modes {
        sealed trait Mode {
            this: Product =>
            val uid = productPrefix
        }
        case object digital extends Mode
        case object analog extends Mode
        case object pwm extends Mode

        def from(cfg: Config) = cfg.getString("mode") match {
            case digital.uid => digital
            case analog.uid => analog
            case pwm.uid => pwm
        }
    }
    object Directions {
        sealed trait Direction {
            this: Product =>
            val uid = productPrefix
        }
        case object input extends Direction
        case object output extends Direction

        def from(cfg: Config): Direction = cfg.getString("direction") match {
            case input.uid => input
            case output.uid => output
        }
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
            map("layout") = layout.uid

            val pinout = for (pin <- pins) yield {
                val pinmap = collection.mutable.Map[String, AnyRef]()
                pinmap("number") = Int.box(pin)
                pinmap("mode") = modes(pin).uid
                pinmap("direction") = directions(pin).uid
                cvf.fromMap(pinmap)
            }

            map("pins") = cvf.fromIterable(pinout)
            cf.parseMap(map)
        }
    }

    def gpio(fn: PinBuilder => Unit): Config = {
        val b = new PinBuilder
        fn(b)
        b.build
    }

    class DslConsumer {
        var pinFn: PinDef => Unit = _

        def pinFn(fn: PinDef => Unit): DslConsumer = {
            pinFn = fn
            this
        }

        def consume(conf: Config): Unit = {
            conf.getConfigList("pins").map {
                cfg => pinFn(PinDef(cfg.getInt("number"), Modes.from(cfg), Directions.from(cfg)))
            }
        }
    }

    case class PinDef(num: Int, mode: Mode, dir: Direction)


    def main(args: Array[String]) {
        val conf = gpio { pin =>
            pin number 1 digital input
            pin number 2 analog output
            pin number 22 pwm
        }
        println(conf)
        new DslConsumer().pinFn(println(_)).consume(conf)
    }
}
