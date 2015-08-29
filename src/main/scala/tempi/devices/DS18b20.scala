package tempi.devices

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.scalalogging.LazyLogging
import tempi._

import scala.io.Source

class DS18b20(id: String, source: Source) extends Actor with Device with LazyLogging {
    import DS18b20._

    def receive: Receive = {
        case RequestReading() =>
            sender() ! readDevice(source).map(success(_)).getOrElse(failure())
    }

    def success(v: Int) = DS18b20Reading(id, v)
    def failure() = ReadingFailure(id, System.currentTimeMillis())
}

object DS18b20 {
    val regex = """t=(\d+)""".r.unanchored
    def props(id: String, source: Source) = Props(classOf[DS18b20], id, source)
    def apply(id: String, source: Source)(implicit sys: ActorSystem) = sys.actorOf(props(id, source))

    case class DS18b20Reading(dev: String, v: Int) extends Reading {
        val t: Long = System.currentTimeMillis()
        lazy val c: Double = v / 1000
        lazy val f: Double = c * 1.8 + 32
    }

    def readDevice(source: Source): Option[Int] = {
        val lines = source.getLines().toSeq
        if (lines.head.endsWith("YES")) lines.last match {
            case regex(v) => Option(v.toInt)
            case _ => None
        }
        else {
            None
        }
    }
}

