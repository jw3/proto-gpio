package tempi

object tests {

    case class MockReading(val dev: String, val c: Double, val f: Double, val t: Long) extends Reading
    object MockReading {
        def apply(dev: String): MockReading = MockReading(dev, -1, -1, -1)
    }

}
