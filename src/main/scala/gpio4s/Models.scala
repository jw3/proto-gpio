package gpio4s

object Models {
    /**
     * Describes a Pi model
     */
    trait PiModel {
        val pins: List[Int]
    }

    /**
     * Raspberry Pi Model B revision 2 P1 header only
     */
    object bRev2 extends PiModel {
        val pins: List[Int] = 0 to 16 toList
    }

    /**
     * Raspberry Pi Model B revision 2, including P5 header
     */
    object bRev2wP5 extends PiModel {
        val pins = bRev2.pins ::: (17 to 20).toList
    }

    /**
     * Raspberry Pi A+
     */
    object aPlus extends PiModel {
        val pins: List[Int] = bRev2.pins ::: (21 to 29).toList
    }

    /**
     * Raspberry Pi B+
     */
    object bPlus extends PiModel {
        val pins = aPlus.pins
    }

    /**
     * Raspberry Pi 2 Model B
     */
    object pi2b extends PiModel {
        val pins = aPlus.pins
    }
}
