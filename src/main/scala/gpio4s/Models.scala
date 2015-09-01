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
     * Raspberry Pi Model B revision 2, both P1 and P5 headers
     */
    object bRev2wP5 extends PiModel {
        val pins = bRev2.pins ::: (17 to 20).toList
    }

    object aPlus extends PiModel {
        val pins: List[Int] = bRev2.pins ::: (21 to 29).toList
    }

    object bPlus extends PiModel {
        val pins = aPlus.pins
    }

    object pi2 extends PiModel {
        val pins = aPlus.pins
    }
}
