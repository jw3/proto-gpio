package gpio4s

import akka.actor._
import com.typesafe.config.Config
import gpio4s.Models.PiModel
import picfg.PiCfg

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


    def configure(conf: Config): Unit = {
        // reset_pi_if_configured
        import PiCfg.RichPins
        conf.eachPin { p => gpios(p.num) ! Setup(p) }
    }

    //\\ do not forward events - outsiders cannot touch pins //\\
    def receive: Receive = {
        case Configure(c) => configure(c)
        case DigitalWrite(p, v) => gpios(p) ! v

        case Subscribe(p) => subscribers.addBinding(p, sender())
        case Unsubscribe(p) => subscribers.removeBinding(p, sender())
        case e @ DigitalEvent(p, v) => subscribers(p).foreach(_ ! e)

        case _ =>
    }
}
