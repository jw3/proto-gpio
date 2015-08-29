package tempi

import akka.actor._
import com.typesafe.scalalogging.LazyLogging
import tempi.Reader._

import scala.collection.mutable
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class Reader extends Actor with LazyLogging {
    val devices = mutable.Map[String, ActorRef]()
    val subscribers = new mutable.HashMap[String, mutable.Set[ActorRef]] with mutable.MultiMap[String, ActorRef]

    val cancellers = mutable.Map[String, Cancellable]()
    var scheduling = false

    def receive: Receive = {
        case m @ Register(id, device) =>
            logger.info(s"$m")
            devices(id) = device

        case m @ Subscribe(id) =>
            logger.info(s"$m")
            subscribers.addBinding(id, sender())

        case m @ Refresh(id) if devices.contains(id) =>
            logger.info(s"$m")
            devices(id) ! RequestReading()

        case r: Reading =>
            subscribers.get(r.dev).foreach(_.foreach(_ ! r))

        case ReadingFailure(id, t, meta) =>
            logger.warn(s"failed to read DS18b20: id:[$id] meta:[${meta.getOrElse("")}]")

        case m @ ScheduleUpdates(id, interval) if scheduling =>
            cancellers(id) = scheduleUpdate(id, interval)

        case m @ _ =>
            logger.warn(s"unhandled message [$m]")
    }

    def scheduleUpdate(id: String, interval: FiniteDuration) =
        context.system.scheduler.schedule(5 seconds, interval, self, Refresh(id))(context.system.dispatcher)
}

object Reader {
    def props() = Props[Reader]
    def apply()(implicit sys: ActorSystem) = sys.actorOf(props())

    case class Subscribe(id: String)
    case class Refresh(id: String)
    case class Register(id: String, device: ActorRef)
    case class ScheduleUpdates(id: String, interval: FiniteDuration = 30 seconds)
}
