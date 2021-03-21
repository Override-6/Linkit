/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.core.connection.network.cache.map

import fr.`override`.linkit.api.connection.network.cache.{SharedCacheFactory, SharedCacheManager}
import fr.`override`.linkit.api.connection.packet.traffic.{PacketSender, PacketSyncReceiver}
import fr.`override`.linkit.api.connection.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.core.connection.network.cache.HandleableSharedCache
import fr.`override`.linkit.core.connection.network.cache.map.MapModification._
import fr.`override`.linkit.core.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.`override`.linkit.core.local.concurrency.BusyWorkerPool
import fr.`override`.linkit.core.local.utils.{ConsumerContainer, ScalaUtils}
import org.jetbrains.annotations.{NotNull, Nullable}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class SharedMap[K, V](handler: SharedCacheManager, identifier: Long, baseContent: Array[(K, V)], channel: PacketSender with PacketSyncReceiver)
        extends HandleableSharedCache[(K, V)](handler, identifier, channel) {

    private val networkListeners = ConsumerContainer[(MapModification, K, V)]()
    private val collectionModifications = ListBuffer.empty[(MapModification, Any, Any)]

    @volatile private var modCount: Int = 0

    override var autoFlush: Boolean = true

    override def toString: String = LocalMap.toString

    override def flush(): SharedMap.this.type = {
        collectionModifications.foreach(flushModification)
        collectionModifications.clear()
        this
    }

    override def modificationCount(): Int = modCount

    /**
     * (MapModification, _, _) : the kind of modification that were done<p>
     * (_, K, _) : the key affected (is null for mod kinds that does not specify any key such as CLEAR)<p>
     * (_, _, V) : The value affected (is null for mod kinds that does not specify any value such as CLEAR, or REMOVE)<p>
     * */
    def addListener(action: ((MapModification, K, V)) => Unit): this.type = {
        networkListeners += action
        //println(s"networkListeners = ${networkListeners}")
        this
    }

    def removeListener(action: ((MapModification, K, V)) => Unit): this.type = {
        networkListeners -= action
        this
    }

    def get(k: K): Option[V] = LocalMap.get(k)

    def getOrWait(k: K): V = awaitPut(k)

    def apply(k: K): V = LocalMap(k)

    def clear(): Unit = {
        LocalMap.clear()
        addLocalModification(CLEAR, null, null)
    }

    def put(k: K, v: V): Unit = {
        LocalMap.put(k, v)
        addLocalModification(PUT, k, v)
    }

    def contains(k: K): Boolean = LocalMap.contains(k)

    def remove(k: K): Unit = {
        addLocalModification(REMOVE, k, LocalMap.remove(k))
    }

    def mapped[nK, nV](map: (K, V) => (nK, nV)): BoundedMap.Immutable[nK, nV] = {
        LocalMap.createBoundedMap(map)
    }

    def foreach(action: (K, V) => Unit): this.type = {
        LocalMap.foreach(action)
        this
    }

    def foreachKeys(action: K => Unit): this.type = {
        keys.foreach(action)
        this
    }

    def foreachValues(action: V => Unit): this.type = {
        values.foreach(action)
        this
    }

    def values: Iterable[V] = {
        LocalMap.values
    }

    def keys: Iterable[K] = {
        LocalMap.keys
    }

    def iterator: Iterator[(K, V)] = LocalMap.iterator

    def size: Int = iterator.size

    def isEmpty: Boolean = iterator.isEmpty

    def awaitPut(k: K): V = {
        if (contains(k))
            return apply(k)
        //println(s"Waiting key ${k} to be put... (${Thread.currentThread()}")

        val lock = new Object
        var found = false

        val listener: ((MapModification, K, V)) => Unit = t => {
            found = t._2 == k
            //println(s"k = ${k}")
            //println(s"t._2 = ${t._2}")
            //println(s"found = ${found}")
            if (found) lock.synchronized {
                //println(s"Notified lock $lock")
                lock.notifyAll()
            }
        }

        addListener(listener) //Due to hyper parallelized thread execution,
        //the awaited key could be added since the 'found' value has been created.
        BusyWorkerPool.executeRemainingTasks(lock, !(contains(k) || found))
        removeListener(listener)
        //println("Done !")
        apply(k)
    }

    override def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = {
        packet match {
            case ObjectPacket(modPacket: (MapModification, K, V)) => handleNetworkModRequest(modPacket)
        }
    }

    private def handleNetworkModRequest(mod: (MapModification, K, V)): Unit = {
        val modKind: MapModification = mod._1
        val key = mod._2
        val value = mod._3

        val action: LocalMap.type => Unit = modKind match {
            case CLEAR => _.clear()
            case PUT => _.put(key, value)
            case REMOVE => _.remove(key)
        }
        action(LocalMap)

        modCount += 1
        networkListeners.applyAll(mod)
    }

    private def addLocalModification(@NotNull kind: MapModification, @Nullable key: Any, @Nullable value: Any): Unit = {
        if (autoFlush) {
            flushModification((kind, key, value))
            return
        }

        kind match {
            case CLEAR => collectionModifications.clear()
            case PUT | REMOVE => collectionModifications.filterInPlace(m => !((m._1 == PUT || m._1 == REMOVE) && m._2 == key))
        }
        collectionModifications += ((kind, key, value))
    }

    private def flushModification(mod: (MapModification, Any, Any)): Unit = {
        sendRequest(ObjectPacket(mod))
        networkListeners.applyAll(mod.asInstanceOf[(MapModification, K, V)])
        modCount += 1
    }

    override def currentContent: Array[Any] = LocalMap.toArray

    override protected def setCurrentContent(content: Array[(K, V)]): Unit = LocalMap.set(content)

    private object LocalMap {

        type nK
        type nV

        private val mainMap = {
            mutable.Map.from[K, V](baseContent)
        }

        private val boundedCollections = ListBuffer.empty[BoundedMap[K, V, nK, nV]]

        def createBoundedMap[nK, nV](map: (K, V) => (nK, nV)): BoundedMap.Immutable[nK, nV] = {
            val boundedMap = new BoundedMap[K, V, nK, nV](map)
            boundedCollections += boundedMap.asInstanceOf[BoundedMap[K, V, this.nK, this.nV]]
            boundedMap.set(mainMap.toArray)
            boundedMap
        }

        def get(k: K): Option[V] = mainMap.get(k)

        def apply(k: K): V = mainMap(k)

        def clear(): Unit = {
            mainMap.clear()
            foreach(_.clear())
        }

        def put(k: K, v: V): Unit = {
            mainMap.put(k, v)
            foreach(_.put(k, v))
        }

        def set(content: Array[(K, V)]): Unit = {
            mainMap.clear()
            mainMap ++= content
            foreach(_.set(content))
        }

        def remove(k: K): Unit = {
            mainMap.remove(k)
            foreach(_.remove(k))
        }

        def foreach(action: (K, V) => Unit): Unit = {
            mainMap.foreachEntry(action)
        }

        def values: Iterable[V] = {
            mainMap.values
        }

        def keys: Iterable[K] = {
            mainMap.keys
        }

        def contains(key: K): Boolean = mainMap.contains(key)

        def toArray: Array[Any] = mainMap.toArray

        def iterator: Iterator[(K, V)] = mainMap.iterator

        private def foreach(action: BoundedMap.Mutator[K, V] => Unit): Unit = {
            boundedCollections.foreach(action)
        }

        //Only for debug purpose
        override def toString: String = mainMap.toString()
    }
}

object SharedMap {
    def apply[K, V]: SharedCacheFactory[SharedMap[K, V]] = {
        (handler: SharedCacheManager, identifier: Long, baseContent: Array[Any], channel: PacketSender with PacketSyncReceiver) => {
            new SharedMap[K, V](handler, identifier, ScalaUtils.slowCopy(baseContent), channel)
        }
    }

}

