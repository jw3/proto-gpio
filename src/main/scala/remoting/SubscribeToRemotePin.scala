package remoting

import akka.actor._
import com.typesafe.config.ConfigFactory
import gpio4s.pi4j._
import gpio4s.{PiModels, Pi, Subscribe}

/**
 * remote actor subscribes to pi
 * @author wassj
 */
object SubscribeToRemotePin {
    val pin = 1

    def main(args: Array[String]) {
        val config = ConfigFactory.load("/remoting.conf")
        val master = config.getString("test.master")
        val myname = config.getString("test.myname")

        val systemName = config.getString("test.system")
        implicit val system = ActorSystem(systemName)

        val pi: ActorSelection = if (master == "this") {
            ActorSelection(Pi(PiModels.bRev2, Pi4jPinProducer()), "/user/pi")
        }
        else {
            system.actorSelection(s"akka.tcp://$systemName@$master/user/pi")
        }

        val subs = system.actorOf(Props[Subscriber], myname)
        pi.tell(Subscribe(pin), subs)
    }
}

class Subscriber extends Actor {
    def receive: Receive = {
        case _ =>
    }
}
