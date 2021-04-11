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

package fr.linkit.core.connection.network.cache.map

import fr.linkit.api.connection.network.cache.{SharedCacheFactory, SharedCacheManager}
import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.traffic.PacketInjectableContainer
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.network.cache.AbstractSharedCache
import fr.linkit.core.connection.network.cache.map.MapModification._
import fr.linkit.core.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.core.connection.packet.traffic.ChannelScopes
import fr.linkit.core.connection.packet.traffic.channel.request.{RequestBundle, RequestPacketChannel}
import fr.linkit.core.local.concurrency.pool.GenericWorkerController
import fr.linkit.core.local.utils.{ConsumerContainer, ScalaUtils}
import org.jetbrains.annotations.{NotNull, Nullable}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class SharedMap[K, V](handler: SharedCacheManager, identifier: Long,
                      baseContent: Array[(K, V)], channel: RequestPacketChannel)
        extends AbstractSharedCache[(K, V)](handler, identifier, channel) {

    private val networkListeners        = ConsumerContainer[(MapModification, K, V)]()
    private val collectionModifications = ListBuffer.empty[(MapModification, Any, Any)]
    private val controller              = new GenericWorkerController()

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
    def addListener(action: (MapModification, K, V) => Unit): this.type = {
        networkListeners += (triple => action(triple._1, triple._2, triple._3))
        this
    }

    //FIXME remove would not work as we are creating a wrapper for this lambda att add and remove.
    def removeListener(action: (MapModification, K, V) => Unit): this.type = {
        networkListeners -= (triple => action(triple._1, triple._2, triple._3))
        this
    }

    override def link(action: ((K, V)) => Unit): this.type = {
        links += action
        foreach((k, v) => action(k, v))
        this
    }

    def get(k: K): Option[V] = LocalMap.get(k)

    def getOrWait(k: K): V = awaitPut(k)

    def getOrElse(id: K, default: => V): V = {
        get(id).getOrElse(default)
    }

    def apply(k: K): V = LocalMap(k)

    def clear(): Unit = {
        LocalMap.clear()
        addLocalModification(CLEAR, null, null)
    }

    def put(k: K, v: V): Unit = {
        LocalMap.put(k, v)
        links.applyAllLater((k, v))
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

    @workerExecution
    def awaitPut(k: K): V = {
        if (contains(k)) {
            return apply(k)
        }
        //println(s"Waiting key ${k} to be put... (${Thread.currentThread()}")
        controller.waitTask(notifyCondition = contains(k))
        //println("Done !")
        apply(k)
    }

    override protected def handleBundle(bundle: RequestBundle): Unit = {
        bundle.packet.nextPacket[Packet] match {
            case ObjectPacket(modPacket: (MapModification, K, V)) => handleNetworkModRequest(modPacket)
        }
    }

    private def handleNetworkModRequest(mod: (MapModification, K, V)): Unit = {
        val modKind: MapModification = mod._1
        val key                      = mod._2
        val value                    = mod._3

        val action: LocalMap.type => Unit = modKind match {
            case CLEAR  => _.clear()
            case PUT    => _.put(key, value)
            case REMOVE => _.remove(key)
        }
        action(LocalMap)
        //println(s"Received modification: $mod, $this, $hashCode, ${LocalMap.hashCode()}")

        modCount += 1
        controller.notifyAnyThread()
        networkListeners.applyAll(mod)
    }

    private def addLocalModification(@NotNull kind: MapModification, @Nullable key: Any, @Nullable value: Any): Unit = {
        if (autoFlush) {
            flushModification((kind, key, value))
            return
        }

        kind match {
            case CLEAR        => collectionModifications.clear()
            case PUT | REMOVE => collectionModifications.filterInPlace(m => !((m._1 == PUT || m._1 == REMOVE) && m._2 == key))
        }
        collectionModifications += ((kind, key, value))
    }

    private def flushModification(mod: (MapModification, Any, Any)): Unit = {
        //        Thread.dumpStack()
        //println(s"Flushed $mod, $this, $hashCode, ${LocalMap.hashCode()}")
        sendModification(ObjectPacket(mod))
        networkListeners.applyAll(mod.asInstanceOf[(MapModification, K, V)])
        modCount += 1
    }

    private def println(msg: String): Unit = {
        AppLogger.vTrace(s"<$family, $identifier> $msg")
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

        def apply(k: K): V = {
            mainMap(k)
        }

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
        (handler: SharedCacheManager, identifier: Long, baseContent: Array[Any], container: PacketInjectableContainer) => {
            val channel = container.getInjectable(5, ChannelScopes.discardCurrent, RequestPacketChannel)
            new SharedMap[K, V](handler, identifier, ScalaUtils.slowCopy(baseContent), channel)
        }
    }

}

