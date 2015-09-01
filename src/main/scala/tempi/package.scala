

package object tempi {

    case class RequestReading()
    case class ReadingFailure(dev: String, t: Long, meta: Option[String] = None)

    trait Reading {
        def dev: String
        def c: Double
        def f: Double
        def t: Long
    }

    object Reading {
        def unapply(r: Reading) = Option((r.dev, r.c, r.f, r.t))
    }
}
