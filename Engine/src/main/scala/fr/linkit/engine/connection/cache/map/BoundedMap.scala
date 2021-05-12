/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.cache.map

import fr.linkit.engine.connection.cache.map.BoundedMap.{Immutable, Mutator}
import fr.linkit.engine.connection.cache.map.MapModification._
import fr.linkit.engine.local.utils.ConsumerContainer

import scala.collection.mutable

class BoundedMap[K, V, nK, nV](mapper: (K, V) => (nK, nV)) extends Mutator[K, V] with Immutable[nK, nV] {

    private val map       = mutable.Map.empty[Any, nV]
    private val listeners = ConsumerContainer[(MapModification, Any)]()

    override def set(content: Array[(K, V)]): Unit = {
        map.clear()
        content.foreach(entry => put(entry._1, entry._2))
    }

    override def put(k: K, v: V): Unit = {
        change(k, v, map.put)
        listeners.applyAll((PUT, k))
    }

    def change(k: K, v: V, action: (nK, nV) => Unit): Unit = {
        val entry  = mapper(k, v)
        val nK: nK = entry._1
        val nV: nV = entry._2
        action(nK, nV)
    }

    override def remove(k: K): Unit = {
        listeners.applyAll((REMOVE, k))
        map.remove(k)
    }

    override def clear(): Unit = {
        map.clear()
        listeners.applyAll((CLEAR, head._1))
    }

    override def get(any: Any): Option[nV] = map.get(any)

    override def apply(any: Any): nV = map(any)

    override def contains(any: Any): Boolean = map.contains(any)

    override def addListener(callback: (MapModification, Any) => Unit): Unit = {
        listeners += (tuple2 => callback(tuple2._1, tuple2._2))
    }

    override def iterator: Iterator[(nK, nV)] = map.iterator.asInstanceOf[Iterator[(nK, nV)]]
}

object BoundedMap {

    trait Mutator[K, V] {

        def set(content: Array[(K, V)]): Unit

        def put(k: K, v: V): Unit

        def remove(k: K): Unit

        def clear(): Unit
    }

    trait Immutable[K, V] extends Iterable[(K, V)] {

        def get(any: Any): Option[V]

        def apply(any: Any): V

        def contains(any: Any): Boolean

        def addListener(callback: (MapModification, Any) => Unit): Unit

    }

}
