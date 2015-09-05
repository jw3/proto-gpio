import anno.RdfProp
import org.openrdf.model.IRI

import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}

// the mmmap
val mmap = collection.mutable.Map[IRI,AnyRef]()

case class Bean(@RdfProp("1") val a: Int, b: String, c: Boolean) {

    def this(n: Boolean) = this(1, "", n)
    def this() = this(1, "", false)
    def this(x: Int, y: Int, z: Int, a: String, b: String, c: String) = this(1, "", false)

    @RdfProp("2") var x: Double = _
}
object Bean {
    def apply(): Bean = Bean(0, "", false)
}

val ts = typeOf[Bean]

val accessors = ts.members.map(_.asTerm).filter(x => x.isVal || x.isVar).map { x =>
    val setter = if (x.setter != NoSymbol) Option(x) else None
    (x -> (x.getter -> setter))
}
accessors.foreach(println)
val accessByName = accessors.map(t => t._1.name -> t._2)
accessByName.foreach(println)
val annotations = accessors.map(_._1.annotations)
val ctors = ts.members.filter(_.isConstructor).map(_.asMethod)
ctors.foreach(println)

/*
    why are paramLists in List(List()) form?
    http://www.scala-lang.org/api/2.11.0/scala-reflect/index.html#scala.reflect.api.Symbols$MethodSymbol
    Can be used to distinguish nullary methods and methods with empty parameter lists. For a nullary method, returns the empty list (i.e. List()). For a method with an empty parameter list, returns a list that contains the empty list (i.e. List(List())).
 */
val ctorParams = ctors.map(_.asMethod).map { c =>
    (c -> c.paramLists.head)
}
ctorParams.foreach(println)


/*
model a simple class with single property

things to capture
- class annotation for the type uri
-


 */
