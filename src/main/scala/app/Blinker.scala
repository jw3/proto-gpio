package app

import akka.actor._
import gpio4s._
import gpio4s.pi4j._
import picfg.PiCfg.Directions.output

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

/**
 * A simple blinking light
 */
object Blinker extends App {
    implicit val system = ActorSystem("app01")
    val pinNum = 8

    val pi = Pi(Models.bRev2, pi4jPins())
    pi ! Configure({ pin =>
        pin number pinNum digital output
    })

    system.scheduler.schedule(0 seconds, 5 seconds, pi, DigitalWrite(pinNum, true))
    system.scheduler.schedule(2 seconds, 5 seconds, pi, DigitalWrite(pinNum, false))
}
