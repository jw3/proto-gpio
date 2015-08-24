package gpio4s

import akka.actor._
import gpio4s.Models.PiModel
import picfg.PiCfg
import PiCfg.Directions._
import com.typesafe.config.Config

import scala.collection.mutable

object Pi {
    def apply(m: PiModel, f: PinProducer)(implicit sys: ActorSystem): ActorRef = sys.actorOf(Props(new Pi(m, f)))
}

class Pi(m: PiModel, f: PinProducer) extends Actor {
    val subscribers = new mutable.HashMap[Int, mutable.Set[ActorRef]] with mutable.MultiMap[Int, ActorRef]

    // init
    val gpio = m.pins.map { num => num -> f.get(num) }.toMap

    def configure(conf: Config): Unit = {
        // reset_pi_if_configured
        import PiCfg.RichPins
        conf.eachPin { pin =>
            gpio(pin.num) ! pin.mode match {
                case digital if pin.dir == input => AsDigitalIn()
                case digital if pin.dir == output => AsDigitalOut()
            }
        }
    }

    def receive: Receive = {
        case Configure(c) => configure(c)
        case e @ DigitalEvent(p, v) => subscribers(p).foreach(_ ! e)
        case DigitalWrite(p, v) => gpio(p) forward v
        case Subscribe(p) => subscribers.addBinding(p, sender())
        case Unsubscribe(p) => subscribers.removeBinding(p, sender())
        case _ =>
    }
}

trait PinProducer {
    def get(num: Int)(implicit context: ActorContext): ActorRef
}
