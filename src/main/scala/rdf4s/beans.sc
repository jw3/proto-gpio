import scala.reflect.runtime.universe.{NoSymbol, _}

case class Bean(a: Int, b: String, c: Boolean) {

    def this(n: Boolean) = this(1, "", n)

    var x: Double = _
}
object Bean {
    def apply(): Bean = Bean(0, "", false)
}

val ts = typeOf[Bean]


 ts.members.map(_.asTerm).filter(x => x.isVal || x.isVar).map { x =>
    val setter = if (x.setter != NoSymbol) Option(x) else None
    (x -> (x.getter -> setter))
}.foreach(println)

ts.members.filter(_.isConstructor).map(_.asMethod).foreach(println)
ts.companion.members.filter(_.isConstructor).map(_.asMethod).foreach(_.para)

