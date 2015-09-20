package devices

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import devices.RelaySS1982a._
import gpio4s.{Device, DeviceInfo, DigitalWrite, PinAllocation}
import net.ceedubs.ficus.Ficus._
import picfg.Directions.input


object RelaySS1982a {
    sealed trait Port
    object In1 extends Port
    object In2 extends Port
    object In3 extends Port
    object In4 extends Port

    case class Engage(p: Port*)
    case class Release(p: Port*)

    def port(num: Int): Port = num match {
        case 1 => In1
        case 2 => In2
        case 3 => In3
        case 4 => In4
        case _ =>
            throw new IllegalArgumentException("num should be between 1-4")
    }

    def info(rid: String, sconf: Config) = {
        import picfg._
        val portsToPins = portToPin(sconf.getConfig(s"thermo.$rid"))

        new DeviceInfo() {
            def id: String = rid
            def impl: Class[_ <: Device] = classOf[RelaySS1982a]
            def conf: Config = gpio(pin => portsToPins.values.foreach(pin number _ digital input)).withFallback(sconf)
        }
    }

    // this could be moved out of the 4port relay
    def portToPin(conf: Config): Map[Port, Int] = {
        val count = conf.as[Int]("ports.count")
        val pairs = for (i <- 1 to count) yield {
            port(i) -> conf.as[Int](s"ports.$i.pin")
        }
        pairs.toMap
    }
}

/**
 * Sain Smart four channel relay (1982a)
 * This relay closes when pulled low, and opens when high or off.
 * @author wassj
 */
class RelaySS1982a(id: String, conf: Config, pins: PinAllocation) extends Device with LazyLogging {
    val portsToPins = portToPin(conf.getConfig(s"thermo.$id"))
    val portsToGPIO = portsToPins.map(v => v._1 -> pins(v._2))
    logger.trace(s"creating SS1982a relay with config[$conf]")

    // todo should register listener on each pin to track state changes

    def receive: Receive = {
        case Engage(p @ _*) => p.distinct.map(portsToGPIO(_)).foreach(_ ! DigitalWrite(-1, false))
        case Release(p @ _*) => p.distinct.map(portsToGPIO(_)).foreach(_ ! DigitalWrite(-1, true))
    }

    override def preStart(): Unit = {
        // disable all pins (high) on start
        portsToGPIO.values.foreach(_ ! DigitalWrite(-1, true))
    }
}
