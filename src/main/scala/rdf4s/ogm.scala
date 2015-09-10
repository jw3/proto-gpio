package rdf4s

import com.typesafe.scalalogging.LazyLogging
import org.openrdf.model._
import org.openrdf.model.vocabulary.RDF
import rdf4s.implicits.RichModel

import scala.collection.JavaConversions._
import scala.reflect.runtime.{currentMirror => mirror}
import scala.reflect.{ClassTag, _}

object ogm extends LazyLogging {

    def write[T: ClassTag](t: T)(implicit mm: Metamodel, mf: ModelFactory, vf: ValueFactory): Option[Model] = {
        mm.query(t.getClass, MetamodelQueries.getters).map { getters =>
            val s = iri("ns:", random)
            val tm = mirror.reflect(t)
            getters.foldLeft(model()) { (rdf, g) =>
                Option(tm.reflectMethod(g).apply()).collectFirst {
                    case v: scala.collection.Iterable[_] => v.map(value)
                    case v: java.lang.Iterable[_] => v.map(value)
                    case v if (mm.tmap.contains(v.getClass)) =>
                        logger.trace(s"!!!! need to recurse for ${v.getClass.getSimpleName}")
                        Seq()
                    case v => Seq(value(v))
                }.map { vals =>
                    val p = mm.smap(g).asInstanceOf[IRI] // assert this for now
                    vals.foreach(rdf.add(s, p, _))
                }
                rdf
            }
        }
    }

    def read[T: ClassTag](id: IRI, m: Model)(implicit mm: Metamodel): Option[T] = {
        m.filter(id, null, null)
        val x = m.filter(id, RDF.TYPE, null).resourceObjects().flatMap(o4t(_))
        mm.query(classTag[T].runtimeClass, MetamodelQueries.ctors)
        None
    }

    def o4t(t: Resource)(implicit mm: Metamodel): Option[AnyRef] = {
        // picking a ctor;
        //
        None
    }
}
