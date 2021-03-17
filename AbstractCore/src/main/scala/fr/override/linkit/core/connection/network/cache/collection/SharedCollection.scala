package fr.`override`.linkit.core.connection.network.cache.collection

import fr.`override`
import fr.`override`.linkit
import fr.`override`.linkit.core
import fr.`override`.linkit.core.connection
import fr.`override`.linkit.core.connection.network
import fr.`override`.linkit.core.connection.network.cache
import fr.`override`.linkit.core.connection.network.cache.collection
import fr.`override`.linkit.core.connection.packet.traffic
import fr.`override`.linkit.core.connection.packet.traffic.channel
import fr.`override`.linkit.internal.concurrency.RelayThreadPool
import fr.`override`.linkit.skull.connection.network.cache.collection.CollectionModification._
import fr.`override`.linkit.skull.connection.network.cache.collection.SharedCollection.CollectionAdapter
import fr.`override`.linkit.skull.connection.network.cache.collection.{BoundedCollection, CollectionModification}
import fr.`override`.linkit.skull.connection.network.cache.{SharedCacheFactory, SharedCacheHandler}
import fr.`override`.linkit.skull.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.`override`.linkit.skull.connection.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.skull.connection.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.internal.utils.ConsumerContainer
import org.jetbrains.annotations.{NotNull, Nullable}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag
import scala.util.control.NonFatal

class SharedCollection[A <: Serializable : ClassTag](handler: network.cache.SharedCacheHandler,
                                                     identifier: Long,
                                                     adapter: CollectionAdapter[A],
                                                     channel: channel.CommunicationPacketChannel)
        extends cache.HandleableSharedCache[A](handler, identifier, channel) with mutable.Iterable[A] {

    private val collectionModifications = ListBuffer.empty[(collection.CollectionModification, Long, Any)]
    private val networkListeners = ConsumerContainer[(collection.CollectionModification, Long, A)]()

    @volatile private var modCount = 0
    @volatile override var autoFlush: Boolean = true

    override def modificationCount(): Int = modCount

    override def flush(): this.type = this.synchronized {
        collectionModifications.foreach(flushModification)
        collectionModifications.clear()
        this
    }

    override def toString: String = adapter.toString

    override def currentContent: Array[Any] = adapter.get().toArray

    override def iterator: Iterator[A] = adapter.iterator

    override final def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = {
        packet match {
            case modPacket: ObjectPacket => RelayThreadPool.runLaterOrHere {
                handleNetworkModRequest(modPacket)
            }
        }
    }

    override protected def setCurrentContent(content: Array[A]): Unit = set(content)

    def contains(a: Any): Boolean = adapter.contains(a)

    def add(t: A): this.type = {
        adapter.add(t)
        addLocalModification(ADD, -1, t)
        this
    }


    def foreach(action: A => Unit): this.type = {
        adapter.get().foreach(action)
        this
    }

    def add(i: Int, t: A): this.type = {
        adapter.insert(i, t)
        addLocalModification(ADD, i, t)
        this
    }

    def set(i: Int, t: A): this.type = {
        addLocalModification(SET, i, adapter.set(i, t))
        this
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
        addLocalModification(REMOVE, i, adapter.remove(i))
        this
    }

    def remove(any: Any): this.type = {
        remove(adapter.get().indexOf(any))
    }

    def clear(): this.type = {
        adapter.clear()
        addLocalModification(CLEAR, -1, null)
        this
    }

    def mapped[B](map: A => B): collection.BoundedCollection.Immutable[B] = {
        adapter.createBoundedCollection(map)
    }

    /**
     * (CollectionModification, _, _) : the kind of modification that were done
     * (_, Int, _) : the index affected (may be -1 for mod kinds that does not specify any index such as CLEAR)
     * (_, _, T) : The object affected (may be null for mod kinds that does not specify any object such as CLEAR, or REMOVE)
     * */
    def addListener(action: (collection.CollectionModification, Long, A) => Unit): this.type = {
        networkListeners += (tuple3 => action(tuple3._1, tuple3._2, tuple3._3))
        this
    }

    private def addLocalModification(@NotNull kind: collection.CollectionModification, @Nullable index: Int, @Nullable value: Any): Unit = {
        if (autoFlush) {
            flushModification((kind, index, value))
            return
        }

        kind match {
            case CLEAR => collectionModifications.clear()
            case SET | REMOVE => collectionModifications.filterInPlace(m => !((m._1 == SET || m._1 == REMOVE) && m._2 == index))
            case ADD => //Do not optimise : the addition result may be different according to the order
        }
        collectionModifications += ((kind, index, value))
    }

    private def flushModification(mod: (collection.CollectionModification, Long, Any)): Unit = {
        sendRequest(ObjectPacket(mod))
        networkListeners.applyAllAsync(mod.asInstanceOf[(collection.CollectionModification, Long, A)])
        modCount += 1
        //println(s"<$family> COLLECTION IS NOW (local): " + adapter + " IDENTIFIER : " + identifier)
    }

    private def handleNetworkModRequest(packet: ObjectPacket): Unit = {
        val mod: (collection.CollectionModification, Long, Any) = packet.casted
        val modKind: collection.CollectionModification = mod._1
        val index = mod._2.toInt
        lazy val item: A = mod._3.asInstanceOf[A] //Only instantiate value if needed
        val action: CollectionAdapter[A] => Unit = modKind match {
            case CLEAR => _.clear()
            case SET => _.set(index, item)
            case REMOVE => _.remove(index)
            case ADD => if (index < 0) _.add(item) else _.insert(index, item)
        }

        try {
            action(adapter)
        } catch {
            case NonFatal(e) => e.printStackTrace()
        }
        modCount += 1

        networkListeners.applyAllAsync(mod.asInstanceOf[(collection.CollectionModification, Long, A)])
    }
}

object SharedCollection {

    private type S[A] = mutable.Seq[A] with mutable.Growable[A] with mutable.Buffer[A]

    def set[A <: Serializable : ClassTag]: SharedCacheFactory[SharedCollection[A]] = {
        ofInsertFilter[A]((coll, it) => {
            val b = !coll.contains(it)
            println(s"Insert filter result : ${b} (collection: $coll, it: $it)")
            b
        })
    }

    def buffer[A  <: Serializable : ClassTag]: SharedCacheFactory[SharedCollection[A]] = {
        ofInsertFilter[A]((_, _) => true)
    }

    def apply[A  <: Serializable : ClassTag]: SharedCacheFactory[SharedCollection[A]] = buffer[A]

    /**
     * The insertFilter must be true in order to authorise the insertion
     * */
    def ofInsertFilter[A  <: Serializable : ClassTag](insertFilter: (CollectionAdapter[A], A) => Boolean): SharedCacheFactory[SharedCollection[A]] = {
        (handler: fr.`override`.linkit.core.connection.network.cache.SharedCacheHandler, identifier: Long, baseContent: Array[Any], channel: traffic.channel.CommunicationPacketChannel) => {
            var adapter: CollectionAdapter[A] = null
            adapter = new CollectionAdapter[A](baseContent.asInstanceOf[Array[A]], insertFilter(adapter, _))

            new SharedCollection[A](handler, identifier, adapter, channel)
        }
    }

    /**
     * The insertFilter must be true in order to authorise the insertion
     * */
    class CollectionAdapter[A](baseContent: Array[A], insertFilter: A => Boolean) extends mutable.Iterable[A] {

        type X
        private val mainCollection = ListBuffer.from(baseContent)
        private val boundedCollections: ListBuffer[linkit.core.connection.network.cache.collection.BoundedCollection[A, X]] = ListBuffer.empty

        override def toString: String = mainCollection.toString()

        override def iterator: Iterator[A] = mainCollection.iterator

        def createBoundedCollection[B](map: A => B): connection.network.cache.collection.BoundedCollection.Immutable[B] = {
            val boundedCollection = new `override`.linkit.core.connection.network.cache.collection.BoundedCollection[A, B](map)
            boundedCollections += boundedCollection.asInstanceOf[fr.`override`.linkit.core.connection.network.cache.collection.BoundedCollection[A, X]]

            val mainContent = mainCollection.toArray[Any].asInstanceOf[Array[A]]
            boundedCollection.set(mainContent)
            boundedCollection
        }

        def clear(): Unit = {
            mainCollection.clear()
            foreachCollection(_.clear())
        }


        def set(i: Int, it: A): Unit = {
            mainCollection.update(i, it)
            foreachCollection(_.add(i, it))
        }

        def insert(i: Int, it: A): Unit = {
            if (!insertFilter(it))
                return

            mainCollection.insert(i, it)
            foreachCollection(_.add(i, it))
        }

        def add(it: A): Int = {
            if (!insertFilter(it))
                return -1

            mainCollection += it
            foreachCollection(_.add(it))
            mainCollection.size - 1
        }


        def set(array: Array[A]): Unit = {
            mainCollection.clear()
            mainCollection ++= array
            foreachCollection(_.set(array))
        }

        def remove(i: Int): A = {
            val removed = mainCollection.remove(i)
            foreachCollection(_.remove(i))
            removed
        }

        def contains(a: Any): Boolean = mainCollection.contains(a)

        private[SharedCollection] def get(): S[A] = mainCollection

        private def foreachCollection(action: core.connection.network.cache.collection.BoundedCollection.Mutator[A] => Unit): Unit =
            RelayThreadPool.runLaterOrHere {
                boundedCollections.clone.foreach(action)
            }
    }

}