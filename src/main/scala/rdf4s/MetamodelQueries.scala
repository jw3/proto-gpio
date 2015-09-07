package rdf4s

import org.openrdf.model.{Model, Resource}
import rdf4s.Metamodel._

import scala.collection.JavaConversions._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}


object MetamodelQueries {

    // ctors with params
    def ctors(tid: Resource, mmodel: Model, lookup: Metamodel.LookupFn): Map[Resource, Seq[Symbol]] = {
        mmodel.filter(null, mmCtor, tid).subjects
        .map(cid => cid -> mmodel.filter(null, mmCtorParam, cid).subjects)
        .map(m => m._1 -> m._2.map(lookup(_)))
        .filterNot(m => m._2.contains(None)) /* filter out entries that have params that are not mapped */
        .map(m => m._1 -> m._2.flatten)
        .map(m => m._1 -> m._2.map(_.asInstanceOf[Symbol]).toSeq)
        .toMap
    }

    // get all gets
    def getters(tid: Resource, mmodel: Model, lookup: LookupFn): Seq[Symbol] = {
        mmodel.filter(null, mmProperty, tid).subjects
        .flatMap(aid => mmodel.filter(null, mmPropertyGet, aid).subjects)
        .flatMap(r => lookup(r))
        .map { m => m.asInstanceOf[Symbol]
        }.toSeq
    }
}
