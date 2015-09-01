package devices

import akka.actor.ActorRef
import com.typesafe.config.Config
import devices.RelaySS1982a._
import gpio4s.{Device, DeviceInfo, DigitalWrite, PinAllocation}
import picfg.PiCfg.Directions.input


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

    def info(_id: String, t4: (Int, Int, Int, Int)) = {
        import picfg.PiCfg._

        new DeviceInfo() {
            def id: String = _id
            def impl: Class[_ <: Device] = classOf[RelaySS1982a]
            def conf: Config = gpio { pin =>
                pin number t4._1 digital input
                pin number t4._2 digital input
                pin number t4._3 digital input
                pin number t4._4 digital input
            }
        }
    }
}

/**
 * Sain Smart four channel relay (1982a)
 * This relay closes when pulled low, and opens when high or off.
 * @author wassj
 */
class RelaySS1982a(id: String, conf: Config, pins: PinAllocation) extends Device {
    assert(pins.size == 4)
    val ports: Map[Port, ActorRef] = pins.map(t => port(t._1) -> t._2)

    // todo should register listener on each pin to track state changes

    def receive: Receive = {
        case Engage(p @ _*) => p.distinct.map(ports(_)).foreach(_ ! DigitalWrite(-1, false))
        case Release(p @ _*) => p.distinct.map(ports(_)).foreach(_ ! DigitalWrite(-1, true))
    }

    override def preStart(): Unit = {
        // disable all pins (high) on start
        ports.values.foreach(_ ! DigitalWrite(-1, true))
    }
}
