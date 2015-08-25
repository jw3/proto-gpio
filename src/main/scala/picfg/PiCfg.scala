package picfg

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import picfg.PiCfg.DigitalState.DigitalState
import picfg.PiCfg.Directions.Direction
import picfg.PiCfg.Layouts.Layout
import picfg.PiCfg.Modes.Mode
import picfg.PiCfg.Pulls.Pull

import scala.collection.JavaConversions._

/**
 * @author wassj
 */
object PiCfg {
    case class PinDef(num: Int,
                      mode: Mode,
                      dir: Direction,
                      state: Option[AnyRef],
                      pull: Option[Pull])

    def gpio(fn: PinBuilder => Unit): Config = {
        val b = new PinBuilder
        fn(b)
        b.build
    }

    implicit class RichPins(conf: Config) {
        def pins(): Seq[PinDef] = conf.getConfigList("pins").map { cfg =>
            PinDef(cfg.getInt("number"),
                Modes.from(cfg),
                Directions.from(cfg),
                InitialState.from(cfg),
                Pulls.from(cfg))
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
            def isInput = this == input
            def isOutput = this == output
        }

        def from(cfg: Config): Direction = cfg.getString("direction") match {
            case input.uid => input
            case output.uid => output
        }
    }
    object Pulls {
        case object off extends Pull
        case object up extends Pull
        case object down extends Pull


        sealed trait Pull {
            this: Product =>
            val uid = productPrefix
        }

        def from(cfg: Config): Option[Pull] = cfg.getAs[String]("pull") match {
            case Some(off.uid) => Option(off)
            case Some(up.uid) => Option(up)
            case Some(down.uid) => Option(down)
            case _ => None
        }
    }

    object InitialState {
        def from(cfg: Config): Option[AnyRef] = DigitalState.from(cfg).orElse {
            if (cfg.hasPath("set")) Option(Double.box(cfg.getDouble("set")))
            else None
        }
    }

    object DigitalState {
        case object hi extends DigitalState
        case object low extends DigitalState


        sealed trait DigitalState {
            this: Product =>
            val uid = productPrefix
        }

        def from(cfg: Config): Option[DigitalState] = cfg.getAs[String]("set") match {
            case Some(hi.uid) => Option(hi)
            case Some(low.uid) => Option(low)
            case _ => None
        }
    }

    trait PinNumberBuilder {
        def number(num: Int): PinModeBuilder
    }

    trait PinModeBuilder {
        def digital(d: Direction): DigitalInitializer
        def analog(d: Direction): AnalogInitializer
        def pwm
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


    private class PinBuilder extends PinNumberBuilder with PinModeBuilder with DigitalInitializer with AnalogInitializer {
        import com.typesafe.config.{ConfigFactory => cf, ConfigValueFactory => cvf}

        var num: Int = -1
        var layout: Layout = Layouts.pi4j
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
        def pwm = {
            modes(num) = Modes.pwm
            directions(num) = Directions.output
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
                    _ match {
                        case v: java.lang.Double => pinmap("set") = Double.box(v)
                        case v: DigitalState => pinmap("set") = v.uid
                    }
                }
                pulls.get(pin).foreach { v => pinmap("pull") = v.uid }
                cvf.fromMap(pinmap)
            }

            map("pins") = cvf.fromIterable(pinout)
            cf.parseMap(map)
        }
    }
}
