package akka.gpio.api

import com.pi4j.io.gpio.Pin

trait PiModel {
  val pins: List[Pin]
}
