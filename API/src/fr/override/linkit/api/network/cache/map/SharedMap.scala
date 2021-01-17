package fr.`override`.linkit.api.network.cache.map

import fr.`override`.linkit.api.network.cache.map.MapModification._
import fr.`override`.linkit.api.network.cache.{HandleableSharedCache, ObjectPacket, SharedCacheFactory}
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.{ConsumerContainer, ScalaUtils}
import org.jetbrains.annotations.{NotNull, Nullable}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class SharedMap[K, V](identifier: Int, baseContent: Array[(K, V)], channel: CommunicationPacketChannel) extends HandleableSharedCache(identifier, channel) {

    private val localMap = LocalMap()
    private val networkListeners = ConsumerContainer[(MapModification, K, V)]()
    private val collectionModifications = ListBuffer.empty[(MapModification, Any, Any)]

    @volatile private var modCount: Int = 0

    override var autoFlush: Boolean = true

    override def toString: String = localMap.toString

    override def flush(): SharedMap.this.type = {
        collectionModifications.foreach(flushModification)
        collectionModifications.clear()
        this
    }

    override def modificationCount(): Int = modCount

    /**
     * (MapModification, _, _) : the kind of modification that were done
     * (_, K, _) : the key affected (may be null for mod kinds that does not specify any key such as CLEAR)
     * (_, _, V) : The value affected (may be null for mod kinds that does not specify any value such as CLEAR, or REMOVE)
     * */
    def addListener(action: (MapModification, K, V) => Unit): this.type = {
        networkListeners += (tuple3 => action.apply(tuple3._1, tuple3._2, tuple3._3))
        this
    }

    def get(k: K): Option[V] = localMap.get(k)

    def apply(k: K): V = localMap(k)

    def clear(): Unit = {
        localMap.clear()
        addLocalModification(CLEAR, null, null)
    }

    def put(k: K, v: V): Unit = {
        localMap.put(k, v)
        addLocalModification(PUT, k, v)
    }

    def contains(k: K): Boolean = localMap.contains(k)

    def remove(k: K): Unit = {
        addLocalModification(REMOVE, k, localMap.remove(k))
    }

    def mapped[nK, nV](map: (K, V) => (nK, nV)): BoundedMap.Immutable[nK, nV] = {
        localMap.createBoundedMap(map)
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

        val action: LocalMap => Unit = modKind match {
            case CLEAR => _.clear()
            case PUT => _.put(key, value)
            case REMOVE => _.remove(key)
        }
        action(localMap)
        modCount += 1

        networkListeners.applyAll(mod)
        println("MAP IS NOW (network): " + localMap + " IDENTIFIER : " + identifier)
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
        println("MAP IS NOW (local): " + localMap + " IDENTIFIER : " + identifier)
    }

    case class LocalMap() {

        type nK
        type nV

        private val mainMap = mutable.Map[K, V](baseContent: _*)
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

        def contains(key: K): Boolean = mainMap.contains(key)

        def toArray: Array[Any] = mainMap.toArray

        private def foreach(action: BoundedMap.Mutator[K, V] => Unit): Unit = {
            boundedCollections.foreach(action)
        }

        //Only for debug purpose
        override def toString: String = mainMap.toString()
    }

    override def currentContent: Array[Any] = localMap.toArray
}

object SharedMap {
    def apply[K, V]: SharedCacheFactory[SharedMap[K, V]] = {
        new SharedCacheFactory[SharedMap[K, V]] {

            override def createNew(identifier: Int, baseContent: Array[AnyRef], channel: CommunicationPacketChannel): SharedMap[K, V] = {
                new SharedMap[K, V](identifier, ScalaUtils.cloneArray(baseContent), channel)
            }

            override def sharedCacheClass: Class[SharedMap[K, V]] = classOf[SharedMap[K, V]]
        }
    }
}

