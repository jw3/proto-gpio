package akka.gpio

import com.pi4j.io.gpio.RaspiPin._

object Models {
    /**
     * Raspberry Pi Model B revision 2 P1 header only
     */
    object bRev2 extends PiModel {
        val pins = List(GPIO_00, GPIO_01, GPIO_02, GPIO_03, GPIO_04, GPIO_05, GPIO_06, GPIO_07, GPIO_08, GPIO_09, GPIO_10, GPIO_11, GPIO_12, GPIO_13, GPIO_14, GPIO_15, GPIO_16)
    }

    /**
     * Raspberry Pi Model B revision 2, both P1 and P5 headers
     */
    object bRev2wP5 extends PiModel {
        val pins = bRev2.pins ::: List(GPIO_17, GPIO_18, GPIO_19, GPIO_20)
    }

}
