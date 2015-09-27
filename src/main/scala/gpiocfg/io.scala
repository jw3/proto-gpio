package gpiocfg

import com.typesafe.config.Config
import gpiocfg.dsl.Modes.{pwm, analog, digital}
import gpiocfg.dsl._
import net.ceedubs.ficus.Ficus._
import scala.collection.JavaConversions._


/**
 * @author wassj
 */
object io {

    case class PinCfg(num: Int,
                      mode: Mode,
                      dir: Direction,
                      state: Option[AnyRef],
                      pull: Option[Pull]) 

    implicit class RichPins(conf: Config) {
        def pins(): Seq[PinCfg] = conf.getConfigList("pins").map { cfg =>
            PinCfg(cfg.getInt("number"),
                modes(cfg),
                direction(cfg),
                initialState(cfg),
                pulls(cfg))
        }

        def eachPin(fn: PinCfg => Unit): Config = {
            pins.foreach(fn)
            conf
        }
    }

    private[gpiocfg] def layout(str: String): Option[Layout] = str match {
        case pi4j.uid => Option(pi4j)
        case bcom.uid => Option(bcom)
        case _ => None
    }

    private[gpiocfg] def modes(cfg: Config) = cfg.getString("mode") match {
        case digital.uid => digital
        case analog.uid => analog
        case pwm.uid => pwm
    }

    private[gpiocfg] def direction(cfg: Config): Direction = cfg.getString("direction") match {
        case input.uid => input
        case output.uid => output
    }

    private[gpiocfg] def pulls(cfg: Config): Option[Pull] = cfg.getAs[String]("pull") match {
        case Some(off.uid) => Option(off)
        case Some(up.uid) => Option(up)
        case Some(down.uid) => Option(down)
        case _ => None
    }

    private[gpiocfg] def initialState(cfg: Config): Option[AnyRef] = digitalState(cfg).orElse {
        if (cfg.hasPath("set")) Option(Double.box(cfg.getDouble("set")))
        else None
    }

    private[gpiocfg] def digitalState(cfg: Config): Option[DigitalState] = cfg.getAs[String]("set") match {
        case Some(hi.uid) => Option(hi)
        case Some(low.uid) => Option(low)
        case _ => None
    }
}
