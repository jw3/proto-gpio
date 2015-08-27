package gpio4s

object Models {
    /**
     * Describes a Pi model
     * @author  wassj
     */
    trait PiModel {
        val pins: List[Int]
    }

    /**
     * Raspberry Pi Model B revision 2 P1 header only
     * @author  wassj
     */
    object bRev2 extends PiModel {
        val pins: List[Int] = 0 to 16 toList
    }

    /**
     * Raspberry Pi Model B revision 2, both P1 and P5 headers
     * @author  wassj
     */
    object bRev2wP5 extends PiModel {
        val pins = bRev2.pins ::: (17 to 20).toList
    }

}
