package fr.`override`.linkit.api.utils.cache

import fr.`override`.linkit.api.packet.channel.AsyncPacketChannel
import fr.`override`.linkit.api.packet.collector.AsyncPacketCollector
import fr.`override`.linkit.api.packet.fundamental.DataPacket
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.cache.CollectionModification._
import fr.`override`.linkit.api.utils.{ConsumerContainer, WrappedPacket}
import org.jetbrains.annotations.{NotNull, Nullable}

import scala.collection.mutable.ListBuffer

abstract class SharedCollection[A] protected() extends SharedCache {

    private val collectionModifications = ListBuffer.empty[(CollectionModification, Int, Any)]
    protected val id: Int //For Debug only
    private val localCollection: SharedCollection.this.LocalCollection = new LocalCollection
    override var autoFlush: Boolean = true
    @volatile private var modCount = 0
    private val networkListeners = ConsumerContainer[(CollectionModification, Int, A)]()

    start()
    @volatile private var initialised = false

    override def modificationCount(): Int = modCount

    override def flush(): this.type = this.synchronized {
        collectionModifications.foreach(flushModification)
        println(collectionModifications)
        collectionModifications.clear()
        this
    }

    override def toString: String = localCollection.toString

    def add(t: A): this.type = {
        addLocalModification(ADD, localCollection.add(t), t)
        this
    }

    def add(i: Int, t: A): this.type = {
        localCollection.add(i, t)
        addLocalModification(ADD, i, t)
        this
    }

    def set(i: Int, t: A): this.type = {
        addLocalModification(SET, i, localCollection.set(i, t))
        this
    }

    private def addLocalModification(@NotNull kind: CollectionModification, @Nullable index: Int, value: Any): Unit = {
        if (autoFlush) {
            flushModification(Tuple3(kind, index, value))
            return
        }

        kind match {
            case CLEAR => collectionModifications.clear()
            case SET | REMOVE => collectionModifications.filterInPlace(m => !(m._1 == SET && m._2 == index))
            case ADD => //Do not optimise : the addition result may be different according to the order
        }
        collectionModifications += (Tuple3(kind, index, value))
    }

    private def flushModification(mod: (CollectionModification, Int, Any)): Unit = {
        broadcastPacket(ObjectPacket(mod))
        networkListeners.applyAll(mod.asInstanceOf[(CollectionModification, Int, A)])
        modCount += 1
        println("COLLECTION IS NOW (local): " + localCollection + s" id : $id")
    }

    def set(content: Array[A]): this.type = {
        clear()
        var i = -1
        content.foreach({
            i += 1
            add(i, _)
        })
        this
    }

    def remove(i: Int): this.type = {
        addLocalModification(REMOVE, i, localCollection.remove(i))
        this
    }

    def clear(): this.type = {
        localCollection.clear()
        addLocalModification(CLEAR, -1, null)
        this
    }

    def mapped[B](map: A => B): BoundedCollection.Immutable[B] = {
        localCollection.createBoundedCollection(map)
    }

    /**
     * (CollectionModification, _, _) => the kind of modification that were done
     * (_, Int, _) => the index affected (may be -1 for mod kinds that does not specify any index such as CLEAR)
     * (_, _, T) => The object affected (may be null for mod kinds that does not specify any object such as CLEAR, or REMOVE)
     * */
    def addListener(action: (CollectionModification, Int, A) => Unit): this.type = {
        networkListeners += (tuple3 => action.apply(tuple3._1, tuple3._2, tuple3._3))
        this
    }

    def awaitInitialised(): this.type = {
        if (!initialised) {
            localCollection.synchronized {
                localCollection.wait()
            }
        }
        this
    }

    protected def sendPacket(packet: Packet, target: String): Unit

    protected def broadcastPacket(packet: Packet): Unit

    protected def initPacketHandling(): Unit

    final protected def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = {
        packet match {
            case modPacket: ObjectPacket => handleNetworkModRequest(modPacket)

            case DataPacket("Init", _) =>
                val array: Array[A] = localCollection.toArray
                sendPacket(WrappedPacket("InitBack", ObjectPacket(array)), coords.senderID)

            case WrappedPacket("InitBack", ObjectPacket(array: Array[A])) => if (!initialised) {
                localCollection.set(array)
                localCollection.synchronized {
                    localCollection.notifyAll()
                }
                initialised = true
                println("COLLECTION IS INITIALISED AS : " + localCollection + s" id : $id")
            }
        }
    }

    private def handleNetworkModRequest(packet: ObjectPacket): Unit = {
        val mod: (CollectionModification, Int, Any) = packet.casted

        val modKind: CollectionModification = mod._1
        val index: Int = mod._2
        lazy val item: A = mod._3.asInstanceOf[A] //Only instantiate value if needed

        val action: LocalCollection => Unit = modKind match {
            case CLEAR => _.clear()
            case SET => _.set(index, item)
            case REMOVE => _.remove(index)
            case ADD => if (index < 0) _.add(item) else _.add(index, item)
        }
        action(localCollection)
        modCount += 1

        networkListeners.applyAll(mod.asInstanceOf[(CollectionModification, Int, A)])
        println("COLLECTION IS NOW (network): " + localCollection + s" id : $id")

    }

    private def start(): Unit = {
        initPacketHandling()
        broadcastPacket(DataPacket("Init"))
    }

    class LocalCollection {
        type X
        private val mainCollection: ListBuffer[A] = ListBuffer.empty[A]
        private val boundedCollections: ListBuffer[BoundedCollection[A, X]] = ListBuffer.empty

        def createBoundedCollection[B](map: A => B): BoundedCollection.Immutable[B] = {
            val boundedCollection = new BoundedCollection[A, B](map)
            boundedCollections += boundedCollection.asInstanceOf[BoundedCollection[A, X]]
            boundedCollection.set(mainCollection.toArray[Any].asInstanceOf[Array[A]])
            boundedCollection
        }

        def clear(): Unit = {
            mainCollection.clear()
            foreach(_.clear())
        }

        def set(i: Int, it: A): Unit = {
            mainCollection.update(i, it)
            foreach(_.add(i, it))
        }

        def add(i: Int, it: A): Unit = {
            mainCollection.insert(i, it)
            foreach(_.add(i, it))
        }

        private def foreach(action: BoundedCollection.Mutator[A] => Unit): Unit = {
            boundedCollections.foreach(action)
        }

        def add(it: A): Int = {
            mainCollection += it
            foreach(_.add(it))
            mainCollection.size - 1
        }

        def set(array: Array[A]): Unit = {
            mainCollection.clear()
            mainCollection ++= array
            foreach(_.set(array))
        }

        def remove(i: Int): Unit = {
            mainCollection.remove(i)
            foreach(_.remove(i))
        }

        def toArray: Array[A] = mainCollection.toArray[Any].asInstanceOf[Array[A]]

        //Only for debug purpose
        override def toString: String = mainCollection.toString()


    }

}

object SharedCollection {

    def dedicated[A](channelID: Int, boundRelay: String)(implicit traffic: PacketTraffic): SharedCollection[A] = {
        new Dedicated[A](traffic.openChannel(channelID, boundRelay, AsyncPacketChannel))
    }

    def dedicated[A](channel: AsyncPacketChannel): SharedCollection[A] = {
        new Dedicated[A](channel)
    }

    def open[A](channelID: Int)(implicit traffic: PacketTraffic): SharedCollection[A] = {
        new Public[A](traffic.openCollector(channelID, AsyncPacketCollector))
    }

    def open[A](collector: AsyncPacketCollector): SharedCollection[A] = {
        new Public[A](collector)
    }

    private class Dedicated[A](channel: AsyncPacketChannel) extends SharedCollection[A]() {
        override protected val id: Int = channel.identifier

        override def sendPacket(packet: Packet, target: String): Unit = channel.sendPacket(packet)

        override def broadcastPacket(packet: Packet): Unit = channel.sendPacket(packet)

        override def close(): Unit = channel.close()

        override protected def initPacketHandling(): Unit = {
            channel.addOnPacketInjected(handlePacket)
        }
    }

    private class Public[A](collector: AsyncPacketCollector) extends SharedCollection[A]() {
        override protected val id: Int = collector.identifier

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

