import java.util.Date

import org.openrdf.model._
import org.openrdf.model.impl.{LinkedHashModelFactory, SimpleValueFactory}

import scala.collection.JavaConversions._
import scala.util.Random

package object rdf4s {
    def iri(s: String)(implicit f: ValueFactory): IRI = f.createIRI(s)
    def iri(ns: String, ln: String)(implicit f: ValueFactory): IRI = f.createIRI(ns, ln)

    def bnode()(implicit f: ValueFactory): BNode = f.createBNode()
    def bnode(id: String)(implicit f: ValueFactory): BNode = f.createBNode(id)

    def literal(label: String)(implicit f: ValueFactory): Literal = f.createLiteral(label)
    def literal(label: String, lang: String)(implicit f: ValueFactory): Literal = f.createLiteral(label, lang)
    def literal(label: String, datatype: IRI)(implicit f: ValueFactory): Literal = f.createLiteral(label, datatype)

    def value(v: Any)(implicit f: ValueFactory): Value = v match {
        case v: Value => v
        case v: String => f.createLiteral(v)
        case v: Date => f.createLiteral(v)
        case v: Boolean => f.createLiteral(v)
        case v: Byte => f.createLiteral(v)
        case v: Short => f.createLiteral(v)
        case v: Int => f.createLiteral(v)
        case v: Long => f.createLiteral(v)
        case v: Float => f.createLiteral(v)
        case v: Double => f.createLiteral(v)
    }

    def statement(s: Resource, p: IRI, o: Value, g: Option[Resource] = None)(implicit f: ValueFactory): Statement = f.createStatement(s, p, o, g.getOrElse(null))

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
        def >
    }

    def model(stmts: Seq[Statement] = Seq.empty)(implicit m: ModelFactory): Model = {
        val model = m.createEmptyModel()
        model.addAll(stmts)
        model
    }
    def model(fn: ModelBuilder => Unit)(implicit f: ValueFactory, m: ModelFactory): Model = {
        val model = m.createEmptyModel()
        fn(new ModelBuilder(model))
        model
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
            this.o = value(o)
            this
        }
        def ><(g: Resource): finisher = {
            this.g = Option(g)
            this
        }

        def > = model.add(statement(s, p, o, g))
    }

    def random(): String = random(4)
    def random(len: Int): String = Random.alphanumeric.take(len).mkString

    object implicits {
        implicit val valueFactory: ValueFactory = SimpleValueFactory.getInstance
        implicit val modelFactory: ModelFactory = new LinkedHashModelFactory
        implicit def string2iri(s: String)(implicit f: ValueFactory): IRI = f.createIRI(s)

        implicit class RichModel(m: Model) {
            def toSeq(): Seq[Statement] = m.toSet.toSeq

            def resourceObjects(): Set[Resource] =
                m.objects().flatMap(o => if (o.isInstanceOf[Resource]) Option(o.asInstanceOf[Resource]) else None).toSet
        }
    }
}
