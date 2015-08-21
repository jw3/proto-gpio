package akka.gpio

import akka.gpio.Conf.Directions.Direction
import akka.gpio.Conf.Layouts.Layout
import akka.gpio.Conf.Modes.Mode
import com.typesafe.config.Config

import scala.collection.JavaConversions._

/**
 * @author wassj
 */
object Conf {
    case class PinDef(num: Int, mode: Mode, dir: Direction)

    def gpio(fn: PinBuilder => Unit): Config = {
        val b = new PinBuilder
        fn(b)
        b.build
    }

    implicit class RichPins(conf: Config) {
        def pins(): Seq[PinDef] = conf.getConfigList("pins").map { cfg =>
            PinDef(cfg.getInt("number"), Modes.from(cfg), Directions.from(cfg))
        }

        def eachPin(fn: PinDef => Unit): Config = {
            pins.foreach(fn)
            conf
        }
    }


    object Layouts {
        case object pi4j extends Layout
        case object bcom extends Layout


        sealed trait Layout {
            this: Product =>
            val uid = productPrefix
        }
        def from(str: String): Option[Layout] = str match {
            case pi4j.uid => Option(pi4j)
            case bcom.uid => Option(bcom)
            case _ => None
        }
    }
    object Modes {
        case object digital extends Mode
        case object analog extends Mode
        case object pwm extends Mode


        sealed trait Mode {
            this: Product =>
            val uid = productPrefix
        }

        def from(cfg: Config) = cfg.getString("mode") match {
            case digital.uid => digital
            case analog.uid => analog
            case pwm.uid => pwm
        }
    }
    object Directions {
        case object input extends Direction
        case object output extends Direction


        sealed trait Direction {
            this: Product =>
            val uid = productPrefix
        }

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

    private class PinBuilder extends PinNumberBuilder with PinModeBuilder {
        import com.typesafe.config.{ConfigFactory => cf, ConfigValueFactory => cvf}

        var num: Int = -1
        val pins = collection.mutable.Set[Int]()
        val modes = collection.mutable.Map[Int, Mode]()
        val directions = collection.mutable.Map[Int, Direction]()
        var layout: Layout = Layouts.pi4j

        def number(num: Int) = {
            // bounds check valid pin 0-53?
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
}
