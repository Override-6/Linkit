package fr.`override`.linkit.api.network.cache.collection

import fr.`override`.linkit.api.concurrency.RelayWorkerThreadPool
import fr.`override`.linkit.api.network.cache.collection.CollectionModification._
import fr.`override`.linkit.api.network.cache.collection.SharedCollection.CollectionAdapter
import fr.`override`.linkit.api.network.cache.collection.{BoundedCollection, CollectionModification}
import fr.`override`.linkit.api.network.cache.{HandleableSharedCache, ObjectPacket, SharedCacheFactory}
import fr.`override`.linkit.api.packet.traffic.dedicated.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.ConsumerContainer
import org.jetbrains.annotations.{NotNull, Nullable}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class SharedCollection[A](family: String,
                          identifier: Int,
                          adapter: CollectionAdapter[A],
                          channel: CommunicationPacketChannel) extends HandleableSharedCache(family, identifier, channel) with mutable.Iterable[A] {

    private val collectionModifications = ListBuffer.empty[(CollectionModification, Int, Any)]
    private val networkListeners = ConsumerContainer[(CollectionModification, Int, A)]()

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

    def mapped[B](map: A => B): BoundedCollection.Immutable[B] = {
        adapter.createBoundedCollection(map)
    }

    /**
     * (CollectionModification, _, _) : the kind of modification that were done
     * (_, Int, _) : the index affected (may be -1 for mod kinds that does not specify any index such as CLEAR)
     * (_, _, T) : The object affected (may be null for mod kinds that does not specify any object such as CLEAR, or REMOVE)
     * */
    def addListener(action: (CollectionModification, Int, A) => Unit): this.type = {
        networkListeners += (tuple3 => action(tuple3._1, tuple3._2, tuple3._3))
        this
    }

    private def addLocalModification(@NotNull kind: CollectionModification, @Nullable index: Int, @Nullable value: Any): Unit = {
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

    private def flushModification(mod: (CollectionModification, Int, Any)): Unit = {
        sendRequest(ObjectPacket(mod))
        networkListeners.applyAllAsync(mod.asInstanceOf[(CollectionModification, Int, A)])
        modCount += 1
        //println(s"<$family> COLLECTION IS NOW (local): " + adapter + " IDENTIFIER : " + identifier)
    }

    override final def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = {
        packet match {
            case modPacket: ObjectPacket => RelayWorkerThreadPool.smartRun {
                handleNetworkModRequest(modPacket)
            }
        }
    }

    private def handleNetworkModRequest(packet: ObjectPacket): Unit = {
        val mod: (CollectionModification, Int, Any) = packet.casted
        val modKind: CollectionModification = mod._1
        val index: Int = mod._2
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

        networkListeners.applyAllAsync(mod.asInstanceOf[(CollectionModification, Int, A)])
    }

}

object SharedCollection {

    private type S[A] = mutable.Seq[A] with mutable.Growable[A] with mutable.Buffer[A]

    def set[A]: SharedCacheFactory[SharedCollection[A]] = {
        ofInsertFilter[A]((coll, it) => !coll.contains(it))
    }

    def buffer[A]: SharedCacheFactory[SharedCollection[A]] = {
        ofInsertFilter[A]((_, _) => true)
    }

    /**
     * The insertFilter must be true in order to authorise the insertion
     * */
    def ofInsertFilter[A](insertFilter: (CollectionAdapter[A], A) => Boolean): SharedCacheFactory[SharedCollection[A]] = {
        new SharedCacheFactory[SharedCollection[A]] {

            override def createNew(family: String, identifier: Int, baseContent: Array[Any], channel: CommunicationPacketChannel): SharedCollection[A] = {
                var adapter: CollectionAdapter[A] = null
                adapter = new CollectionAdapter[A](baseContent.asInstanceOf[Array[A]], insertFilter(adapter, _))

                new SharedCollection[A](family, identifier, adapter, channel)
            }

            override def sharedCacheClass: Class[SharedCollection[A]] = classOf[SharedCollection[A]]
        }
    }

    /**
     * The insertFilter must be true in order to authorise the insertion
     * */
    class CollectionAdapter[A](baseContent: Array[A], insertFilter: A => Boolean) extends mutable.Iterable[A] {

        type X
        private val mainCollection = ListBuffer.from(baseContent)
        private val boundedCollections: ListBuffer[BoundedCollection[A, X]] = ListBuffer.empty

        override def toString: String = mainCollection.toString()

        override def iterator: Iterator[A] = mainCollection.iterator

        def createBoundedCollection[B](map: A => B): BoundedCollection.Immutable[B] = {
            val boundedCollection = new BoundedCollection[A, B](map)
            boundedCollections += boundedCollection.asInstanceOf[BoundedCollection[A, X]]

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

        private def foreachCollection(action: BoundedCollection.Mutator[A] => Unit): Unit =
            RelayWorkerThreadPool.smartRun {
                boundedCollections.foreach(action)
            }
    }

}