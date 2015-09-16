package rdf4s

import com.typesafe.scalalogging.LazyLogging
import org.openrdf.model._
import org.openrdf.model.vocabulary.RDF

import scala.collection.JavaConversions._
import scala.reflect.runtime.universe._
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
            println(m)
            s -> m
        }
    }

    def allOf[T: ClassTag](m: Model)(implicit mm: Metamodel): Option[Seq[T]] = {
        val t = classTag[T].runtimeClass
        mm.tmap.get(t).map { r =>
            m.filter(null, RDF.TYPE, r)
            .subjects
            .flatMap(read[T](_, m))
            .toSeq
        }
    }

    def read[T: ClassTag](id: Resource, m: Model)(implicit mm: Metamodel): Option[T] = {
        mm.query(classTag[T].runtimeClass, MetamodelQueries.ctors).map { ctors =>
            val ctor = ctors.head
            val args = ctor._2
                       .flatMap(mm.smap.get)
                       .filter(_.isInstanceOf[IRI])
                       .map(_.asInstanceOf[IRI])
                       .flatMap(p => m.filter(id, p, null).objects())
                       .map(devalue)
            o4t[T](ctor._1, args).get
        }
    }

    def o4t[T](tid: Resource, args: Seq[Any])(implicit mm: Metamodel): Option[T] = {
        mm.mmap.get(tid)
        .map(_.asInstanceOf[MethodSymbol])
        .map(ctor => mirror.reflectClass(ctor.owner.asClass)
                     .reflectConstructor(ctor)
                     .apply(args: _*))
        .map(_.asInstanceOf[T])
    }
}
