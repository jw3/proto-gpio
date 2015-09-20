package app

import java.nio.file.Paths

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import devices.RelaySS1982a
import devices.RelaySS1982a.In1
import gpio4s.Device.{DeviceInstallFailed, DeviceInstalled, DeviceMessage, InstallDevice}
import gpio4s.pi4j._
import gpio4s.{PiModels, Pi}
import net.ceedubs.ficus.Ficus._
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore
import rdf4s.Metamodel
import tempi.Reader.{Register, Subscribe}
import tempi.devices.DS18b20
import tempi.{Reader, Reading}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

/**
 * Reference Implementation / Sample Application
 *
 * Model a Thermostat.  There is a single temperature device attached
 * and a threshold assigned.  When the threshold is exceeded an attached
 * relay is engaged until the threshold is within range again.
 */
object Thermostat extends LazyLogging {
    def main(args: Array[String]) {
        val config =
            if (args.length == 1) ConfigFactory.parseFile(Paths.get(s"${args(0)}/thermostat.conf").toFile)
            else ConfigFactory.load("src/main/dist/conf/thermostat.conf")

        implicit val timeout = Timeout(10 seconds)
        implicit val system = ActorSystem("TempMonitorApp")

        logger.trace("starting thermostat")

        // create a new Pi and temp reader
        val pi = Pi(PiModels.bRev2, Pi4jPinProducer())
        val reader = Reader()

        // create a temperature device, and hook it up to the dashboard
        val devId = config.getString("thermo.ds18b20")
        reader ! Register(devId, DS18b20(devId))
        reader.tell(Subscribe(devId), History())

        // install a SainSmart 4channel relay on the pi
        val info = RelaySS1982a.info("relay", config)
        val response = pi ? InstallDevice(info)
        response.mapTo[DeviceMessage].onSuccess {
            case DeviceInstalled(id, ref) =>
                val temp = config.as[Int]("thermo.high")
                val lower = config.as[Int]("thermo.lower")
                reader.tell(Subscribe(devId), RelayUpdater(ref, temp, lower))
            case DeviceInstallFailed(id, reason) =>
                logger.error(s"device $id install failed", reason)
        }
    }

    /**
     * actor that turns readings into physical changes through the attached relay
     */
    class RelayUpdater(relay: ActorRef, desiredTemp: Double, lowerBounds: Double) extends Actor with LazyLogging {
        var burning = false

        def receive: Receive = {
            case Reading(id, c, f, t) =>
                logger.info(s"received temp update [${c}c/${f}f}] for dev[$id] at [$t]")
                if (!burning && c < desiredTemp - lowerBounds) {
                    burning = true
                    relay ! RelaySS1982a.Engage(In1)
                }
                else if (burning && c >= desiredTemp) {
                    relay ! RelaySS1982a.Release(In1)
                    burning = false
                }

        }
    }
    object RelayUpdater {
        def apply(relay: ActorRef, temp: Double, lower: Double)(implicit sys: ActorSystem) =
            sys.actorOf(Props(classOf[RelayUpdater], relay, temp, lower))
    }

    class History extends Actor with LazyLogging {
        import History._
        import rdf4s.implicits.modelFactory

        val repo = new SailRepository(new MemoryStore())
        val conn = {
            repo.initialize()
            repo.getConnection
        }
        implicit val valueFactory = repo.getValueFactory

        def receive: Receive = {
            case Reading(id, c, f, t) =>
                rdf4s.ogm.write(Record(id, c, t)).foreach(r => conn.add(r._2))
                logger.info(s"logging temp update [${c}c/${f}f}] for dev[$id] at [$t]")
        }
    }
    object History {
        def apply()(implicit sys: ActorSystem) = sys.actorOf(Props[History])

        case class Record(id: String, temp: Double, time: Long)

        implicit val mm: Metamodel = {
            val mm = new Metamodel
            mm.install[Record]
            mm
        }
    }
}
