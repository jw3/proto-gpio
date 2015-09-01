package gpio4s

import akka.actor._
import com.typesafe.config.Config
import gpio4s.Device.{DeviceInstallFailed, DeviceInstalled, InstallDevice}
import gpio4s.Models.PiModel
import picfg.PiCfg.RichPins

import scala.collection.mutable


/**
 * Interface to a Pi
 * @author  wassj
 */
object Pi {
    def apply(m: PiModel, f: PinProducer)(implicit sys: ActorSystem): ActorRef = sys.actorOf(Props(new Pi(m, f)))
}

class Pi(m: PiModel, f: PinProducer) extends Actor {
    val gpios = m.pins.map { num => num -> f.get(num) }.toMap
    val subscribers = new mutable.HashMap[Int, mutable.Set[ActorRef]] with mutable.MultiMap[Int, ActorRef]

    def configure(conf: Config, reset: Boolean = false): PinAllocation = {
        val pins = conf.pins()
        pins.foreach(p => gpios(p.num) ! Setup(p))
        pins.map(p => (p.num -> gpios(p.num))).toMap
    }

    //\\ do not forward events - outsiders cannot touch pins //\\
    def receive: Receive = {
        case Configure(c) => configure(c)
        case m @ DigitalWrite(p, _) => gpios(p) ! m

        case Subscribe(p) => subscribers.addBinding(p, sender())
        case Unsubscribe(p) => subscribers.removeBinding(p, sender())
        case e @ DigitalEvent(p, v) => subscribers(p).foreach(_ ! e)

        case m @ InstallDevice(info) =>
            // validate id is installable
            // validate the pins are free
            try {
                val pins = configure(info.conf)
                val dev = context.actorOf(info.props(pins))
                sender() ! DeviceInstalled(info.id, dev)
            }
            catch {
                case t: Throwable =>
                    sender() ! DeviceInstallFailed(info.id, t)
            }
        case _ =>
    }
}
