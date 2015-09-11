package rdf4s

import com.typesafe.scalalogging.LazyLogging
import org.openrdf.model._
import org.openrdf.model.vocabulary.RDF
import rdf4s.implicits.RichModel

import scala.collection.JavaConversions._
import scala.reflect.runtime.{currentMirror => mirror}
import scala.reflect.{ClassTag, _}

object ogm extends LazyLogging {

    def write[T: ClassTag](t: T)(implicit mm: Metamodel, mf: ModelFactory, vf: ValueFactory): Option[(Resource, Model)] = {
        val tclass = t.getClass
        mm.tmap.get(tclass).map { tid =>
            val m = model()
            val s = iri("ns:", random)
            m.add(s, RDF.TYPE, tid)

            val tm = mirror.reflect(t)
            mm.query(tclass, MetamodelQueries.getters).foreach {
                _.foldLeft(m) { (rdf, g) =>
                    val p = mm.smap(g.accessed).asInstanceOf[IRI] // assert this for now
                    Option(tm.reflectMethod(g).apply()).collectFirst {
                        case v: scala.collection.Iterable[_] => v.map(value)
                        case v: java.lang.Iterable[_] => v.map(value)
                        case v if (mm.tmap.contains(v.getClass)) =>
                            val subt = v.getClass
                            write(v)(ClassTag(v.getClass), mm, mf, vf).map { res =>
                                logger.trace(s"$t.$p recurse for $subt")
                                rdf.add(s, p, res._1)
                                rdf.addAll(res._2)
                            }
                            Seq()
                        case v => Seq(value(v))
                    }.map { vals =>
                        vals.foreach(rdf.add(s, p, _))
                    }
                    rdf
                }
            }
            s -> m
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
