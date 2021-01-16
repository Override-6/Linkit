package fr.`override`.linkit.api.utils.cache.map

import fr.`override`.linkit.api.packet.channel.AsyncPacketChannel
import fr.`override`.linkit.api.packet.collector.AsyncPacketCollector
import fr.`override`.linkit.api.packet.fundamental.DataPacket
import fr.`override`.linkit.api.packet.traffic.{PacketInjectable, PacketTraffic}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.cache.map.MapModification._
import fr.`override`.linkit.api.utils.cache.{ObjectPacket, SharedCache}
import fr.`override`.linkit.api.utils.{ConsumerContainer, WrappedPacket}
import org.jetbrains.annotations.{NotNull, Nullable}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class SharedMap[K, V] extends SharedCache with PacketInjectable {
    private val localMap = LocalMap()
    private val networkListeners = ConsumerContainer[(MapModification, K, V)]()
    private val collectionModifications = ListBuffer.empty[(MapModification, Any, Any)]
    override var autoFlush: Boolean = true
    @volatile private var modCount: Int = 0

    start()
    @volatile private var initialised = false

    override def toString: String = localMap.toString

    override def flush(): SharedMap.this.type = {
        collectionModifications.foreach(flushModification)
        this
    }

    override def modificationCount(): Int = modCount

    override def awaitInitialised(): SharedMap.this.type = {
        if (!initialised)
            localMap.synchronized {
                wait()
            }
        this
    }

    /**
     * (MapModification, _, _) : the kind of modification that were done
     * (_, K, _) : the key affected (may be null for mod kinds that does not specify any key such as CLEAR)
     * (_, _, V) : The value affected (may be null for mod kinds that does not specify any value such as CLEAR, or REMOVE)
     * */
    def addListener(action: (MapModification, K, V) => Unit): this.type = {
        networkListeners += (tuple3 => action.apply(tuple3._1, tuple3._2, tuple3._3))
        this
    }


    def start(): Unit = {
        initPacketHandling()
        broadcastPacket(DataPacket("Init"))
    }

    def get(k: K): Option[V] = localMap.get(k)

    def clear(): Unit = {
        localMap.clear()
        addLocalModification(CLEAR, null, null)
    }

    def put(k: K, v: V): Unit = {
        localMap.put(k, v)
        addLocalModification(PUT, k, v)
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
        broadcastPacket(ObjectPacket(mod))
        networkListeners.applyAll(mod.asInstanceOf[(MapModification, K, V)])
        modCount += 1
        println("COLLECTION IS NOW (local): " + localMap)
    }

    def remove(k: K): Unit = {
        addLocalModification(REMOVE, k, localMap.remove(k))
    }

    def mapped[nK, nV](map: (K, V) => (nK, nV)): BoundedMap.Immutable[nK, nV] = {
        localMap.createBoundedMap(map)
    }

    protected def sendPacket(packet: Packet, target: String): Unit

    protected def broadcastPacket(packet: Packet): Unit

    protected def initPacketHandling(): Unit

    final protected def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = {
        packet match {
            case ObjectPacket(modPacket: (MapModification, K, V)) => handleNetworkModRequest(modPacket)

            case DataPacket("Init", _) =>
                val array: Array[(K, V)] = localMap.toArray
                sendPacket(WrappedPacket("InitBack", ObjectPacket(array)), coords.senderID)

            case WrappedPacket("InitBack", ObjectPacket(array: Array[(K, V)])) => if (!initialised) {
                localMap.set(array)
                localMap.synchronized {
                    localMap.notifyAll()
                }
                initialised = true
                println("MAP IS INITIALISED AS : " + localMap)
            }
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
        println("MAP IS NOW (network): " + localMap)
    }

    case class LocalMap() {

        type nK
        type nV

        private val mainMap = mutable.Map.empty[K, V]
        private val boundedCollections = ListBuffer.empty[BoundedMap[K, V, nK, nV]]

        def createBoundedMap[nK, nV](map: (K, V) => (nK, nV)): BoundedMap.Immutable[nK, nV] = {
            val boundedMap = new BoundedMap[K, V, nK, nV](map)
            boundedCollections += boundedMap.asInstanceOf[BoundedMap[K, V, this.nK, this.nV]]
            boundedMap.set(mainMap.toArray)
            boundedMap
        }

        def get(k: K): Option[V] = mainMap.get(k)

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

        private def foreach(action: BoundedMap.Mutator[K, V] => Unit): Unit = {
            boundedCollections.foreach(action)
        }

        def remove(k: K): Unit = {
            mainMap.remove(k)
            foreach(_.remove(k))
        }

        def toArray: Array[(K, V)] = mainMap.toArray

        //Only for debug purpose
        override def toString: String = mainMap.toString()
    }


    
}

object SharedMap {
    def dedicated[K, V](channelID: Int, boundRelay: String)(implicit traffic: PacketTraffic): SharedMap[K, V] = {
        new Dedicated[K, V](traffic.openChannel(channelID, boundRelay, AsyncPacketChannel))
    }

    def dedicated[K, V](channel: AsyncPacketChannel): SharedMap[K, V] = {
        new Dedicated[K, V](channel)
    }

    def open[K, V](channelID: Int)(implicit traffic: PacketTraffic): SharedMap[K, V] = {
        new Public[K, V](traffic.openCollector(channelID, AsyncPacketCollector))
    }

    def open[K, V](collector: AsyncPacketCollector): SharedMap[K, V] = {
        new Public[K, V](collector)
    }

    private class Dedicated[K, V](channel: AsyncPacketChannel) extends SharedMap[K, V]() {

        override def sendPacket(packet: Packet, target: String): Unit = channel.sendPacket(packet)

        override def broadcastPacket(packet: Packet): Unit = channel.sendPacket(packet)

        override def close(): Unit = channel.close()

        override protected def initPacketHandling(): Unit = {
            channel.addOnPacketInjected(handlePacket)
        }
    }

    private class Public[K, V](collector: AsyncPacketCollector) extends SharedMap[K, V]() {

        override def sendPacket(packet: Packet, target: String): Unit = collector.sendPacket(packet, target)

        override def broadcastPacket(packet: Packet): Unit = {
            collector.broadcastPacket(packet)
        }

        override def close(): Unit = collector.close()

        override protected def initPacketHandling(): Unit = {
            collector.addOnPacketInjected(handlePacket)
        }
    }
}

