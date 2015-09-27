package app

import akka.actor._
import gpio4s._
import gpio4s.pi4j._
import gpiocfg.dsl._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

/**
 * A simple blinking light
 */
object Blinker extends App {
    implicit val system = ActorSystem("app01")
    val pinNum = 8

    val pi = Pi(PiModels.bRev2, Pi4jPinProducer())
    pi ! Configure({ pin =>
        pin number pinNum digital output
    })

    system.scheduler.schedule(0 seconds, 5 seconds, pi, DigitalWrite(pinNum, true))
    system.scheduler.schedule(2 seconds, 5 seconds, pi, DigitalWrite(pinNum, false))
}
