import java.util.Date

import org.openrdf.model._
import org.openrdf.model.impl.{LinkedHashModelFactory, SimpleValueFactory}

import scala.util.Random

package object rdf4s {

    // package values?
    def iri(s: String)(implicit f: ValueFactory): IRI = f.createIRI(s)
    def iri(ns: String, ln: String)(implicit f: ValueFactory): IRI = f.createIRI(ns, ln)

    def bnode()(implicit f: ValueFactory): BNode = f.createBNode()
    def bnode(id: String)(implicit f: ValueFactory): BNode = f.createBNode(id)

    def literal(label: String)(implicit f: ValueFactory): Literal = f.createLiteral(label)
    def literal(label: String, lang: String)(implicit f: ValueFactory): Literal = f.createLiteral(label, lang)
    def literal(label: String, datatype: IRI)(implicit f: ValueFactory): Literal = f.createLiteral(label, datatype)

    def literal(v: Date)(implicit f: ValueFactory): Literal = f.createLiteral(v)
    def literal(v: AnyVal)(implicit f: ValueFactory): Literal = v match {
        case v: Boolean => f.createLiteral(v)
        case v: Byte => f.createLiteral(v)
        case v: Short => f.createLiteral(v)
        case v: Int => f.createLiteral(v)
        case v: Long => f.createLiteral(v)
        case v: Float => f.createLiteral(v)
        case v: Double => f.createLiteral(v)
    }

    def statement(s: Resource, p: IRI, o: Value, g: Option[Resource] = None)(implicit f: ValueFactory): Statement = f.createStatement(s, p, o, g.getOrElse(null))


    // <s><p><o>

    trait punct
    object ! extends punct

    trait sb {
        def <(s: String): pb
        def <(s: Resource): pb
    }
    trait pb {
        def ><(p: IRI): ob
        def ><(p: String): ob
    }
    trait ob {
        def ><(o: Value): gb
        def ><(o: AnyVal): gb
    }
    trait gb extends finisher {
        def ><(g: Resource): finisher
    }
    trait finisher {
        def >(): Unit
    }

    def model()(implicit m: ModelFactory): Model = m.createEmptyModel()

    def model(fn: ModelBuilder => Unit)(implicit f: ValueFactory, m: ModelFactory): Model = {
        val model = m.createEmptyModel()
        fn(new ModelBuilder(model))
        model
    }

    {
        import implicits._
        val m: Model = model { add =>
            val p = iri("the:p")
            add < "people:bob" >< p >< 1 >;
            add < "foo:bar" >< "x:has" >< 2 >;
        }
    }


    class ModelBuilder(model: Model)(implicit f: ValueFactory) extends sb with pb with ob with gb with finisher {
        var s: Resource = _
        var p: IRI = _
        var o: Value = _
        var g: Option[Resource] = None

        def <(s: String): pb = {
            this.s = iri(s)
            this
        }
        def <(s: Resource): pb = {
            this.s = s
            this
        }
        def ><(p: String): ob = {
            this.p = iri(p)
            this
        }
        def ><(p: IRI): ob = {
            this.p = p
            this
        }
        def ><(o: Value): gb = {
            this.o = o
            this
        }
        def ><(o: AnyVal): gb = {
            this.o = literal(o)
            this
        }
        def ><(g: Resource): finisher = {
            this.g = Option(g)
            this
        }

        def >(): Unit = model.add(statement(s, p, o, g))
    }


    def random: String = random(4)
    def random(len: Int): String = Random.alphanumeric.take(len).mkString


    object implicits {
        implicit val valueFactory: ValueFactory = SimpleValueFactory.getInstance
        implicit val modelFactory: ModelFactory = new LinkedHashModelFactory
        implicit def string2iri(s: String)(implicit f: ValueFactory): IRI = f.createIRI(s)
    }

    object test {
        import implicits._

        val id1 = iri("foo:bar")
        val id2 = iri("ns", random)
        val id3 = iri("", "")
        val id4 = iri("ns", random(15))

        {
            x("")

            def x(iri: IRI) = ()
        }
    }
}
