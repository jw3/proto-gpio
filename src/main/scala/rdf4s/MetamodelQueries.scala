package rdf4s

import org.openrdf.model.Resource
import rdf4s.Metamodel._

import scala.collection.JavaConversions._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}


object MetamodelQueries {
    // ctors with params
    def ctors: QueryFn[Map[Resource, Seq[Symbol]]] = { (tid, mmodel, lookup) =>
        mmodel.filter(null, mmCtor, tid).subjects
        .map(cid => cid -> mmodel.filter(null, mmCtorParam, cid).subjects)
        .map(m => m._1 -> m._2.map(lookup(_)))
        .filterNot(m => m._2.contains(None)) /* filter out entries that have params of a type that is not mapped */
        .map(m => m._1 -> m._2.flatten)
        .map(m => m._1 -> m._2.map(_.asInstanceOf[Symbol]).toSeq)
        .toMap
    }


    // get all gets
    def getters: QueryFn[Seq[Symbol]] = { (tid, mmodel, lookup) =>
        mmodel.filter(null, mmProperty, tid).subjects
        .flatMap(aid => mmodel.filter(null, mmPropertyGet, aid).subjects)
        .flatMap(r => lookup(r))
        .map { m => m.asInstanceOf[Symbol]
        }.toSeq
    }
}
