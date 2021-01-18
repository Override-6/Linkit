package fr.`override`.linkit.api.network.cache.collection

import fr.`override`.linkit.api.concurency.AsyncExecutionContext
import fr.`override`.linkit.api.network.cache.collection.CollectionModification._
import fr.`override`.linkit.api.network.cache.collection.{BoundedCollection, CollectionModification}
import fr.`override`.linkit.api.network.cache.{HandleableSharedCache, ObjectPacket, SharedCacheFactory}
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.ConsumerContainer
import org.jetbrains.annotations.{NotNull, Nullable}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.control.NonFatal

class SharedCollection[A](family: String, identifier: Int, baseContent: Array[A], channel: CommunicationPacketChannel) extends HandleableSharedCache(family, identifier, channel) {

    private val collectionModifications = ListBuffer.empty[(CollectionModification, Int, Any)]
    private val localCollection = new LocalCollection
    private val networkListeners = ConsumerContainer[(CollectionModification, Int, A)]()

    @volatile private var modCount = 0
    @volatile override var autoFlush: Boolean = true

    override def modificationCount(): Int = modCount

    override def flush(): this.type = this.synchronized {
        collectionModifications.foreach(flushModification)
        collectionModifications.clear()
        this
    }

    override def toString: String = localCollection.toString

    def add(t: A): this.type = {
        addLocalModification(ADD, localCollection.add(t), t)
        this
    }

    def foreach(action: A => Unit): this.type = {
        localCollection.foreach(action)
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
     * (CollectionModification, _, _) : the kind of modification that were done
     * (_, Int, _) : the index affected (may be -1 for mod kinds that does not specify any index such as CLEAR)
     * (_, _, T) : The object affected (may be null for mod kinds that does not specify any object such as CLEAR, or REMOVE)
     * */
    def addListener(action: (CollectionModification, Int, A) => Unit): this.type = {
        networkListeners += (tuple3 => action.apply(tuple3._1, tuple3._2, tuple3._3))
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
        networkListeners.applyAll(mod.asInstanceOf[(CollectionModification, Int, A)])
        modCount += 1
        //println(s"<${family}> COLLECTION IS NOW (local): " + localCollection + " IDENTIFIER : " + identifier)
    }

    override final def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = {
        packet match {
            case modPacket: ObjectPacket => Future {
                handleNetworkModRequest(modPacket)
            }(AsyncExecutionContext)
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
        try {
            action(localCollection)
        } catch {
            case NonFatal(e) => e.printStackTrace()
        }
        modCount += 1

        networkListeners.applyAllAsync(mod.asInstanceOf[(CollectionModification, Int, A)])
        //println(s"<${family}> COLLECTION IS NOW (network): " + localCollection + s" identifier : $identifier")
    }

    class LocalCollection {
        type X
        private val mainCollection: ListBuffer[A] = ListBuffer(baseContent: _*)
        private val boundedCollections: ListBuffer[BoundedCollection[A, X]] = ListBuffer.empty

        override def toString: String = mainCollection.toString()

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

        def add(i: Int, it: A): Unit = {
            mainCollection.insert(i, it)
            foreachCollection(_.add(i, it))
        }

        def add(it: A): Int = {
            mainCollection += it
            foreachCollection(_.add(it))
            mainCollection.size - 1
        }

        def set(array: Array[A]): Unit = {
            mainCollection.clear()
            mainCollection ++= array
            foreachCollection(_.set(array))
        }

        def remove(i: Int): Unit = {
            mainCollection.remove(i)
            foreachCollection(_.remove(i))
        }

        def foreach(action: A => Unit): Unit = mainCollection.foreach(action)

        def toArray: Array[Any] = mainCollection.toArray[Any]

        private def foreachCollection(action: BoundedCollection.Mutator[A] => Unit): Unit = {
            boundedCollections.foreach(action)
        }

    }

    override def currentContent: Array[Any] = localCollection.toArray
}

object SharedCollection {

    def apply[A]: SharedCacheFactory[SharedCollection[A]] = {
        new SharedCacheFactory[SharedCollection[A]] {

            override def createNew(family: String, identifier: Int, baseContent: Array[AnyRef], channel: CommunicationPacketChannel): SharedCollection[A] = {
                new SharedCollection[A](family, identifier, baseContent.asInstanceOf[Array[A]], channel)
            }

            override def sharedCacheClass: Class[SharedCollection[A]] = classOf[SharedCollection[A]]
        }
    }

}

