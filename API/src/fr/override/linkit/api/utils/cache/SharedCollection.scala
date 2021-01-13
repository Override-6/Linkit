package fr.`override`.linkit.api.utils.cache

import fr.`override`.linkit.api.packet.collector.AsyncPacketCollector
import fr.`override`.linkit.api.packet.fundamental.DataPacket
import fr.`override`.linkit.api.utils.cache.CollectionModification._
import fr.`override`.linkit.api.utils.{ConsumerContainer, WrappedPacket}
import org.jetbrains.annotations.{NotNull, Nullable}

import scala.collection.mutable.ListBuffer

class SharedCollection[T](collector: AsyncPacketCollector) extends SharedCache {

    private val collectionModifications = ListBuffer.empty[(CollectionModification, Int, Any)]
    private val collection = ListBuffer.empty[T]
    private val networkListeners = ConsumerContainer[(CollectionModification, Int, T)]()
    override var autoFlush: Boolean = true

    @volatile private var modCount = 0
    @volatile private var uninitialised = true

    def add(t: T): Unit = {
        collection += t
        addLocalModification(ADD, -1, t)
    }

    start()

    override def flush(): Unit = this.synchronized {
        collectionModifications.foreach(flushModification(_))
        collectionModifications.clear()
    }

    override def modificationCount(): Int = modCount

    override def toString: String = collection.toString()

    def add(i: Int, t: T): Unit = {
        collection.insert(i, t)
        addLocalModification(ADD, i, t)
    }

    def set(i: Int, t: T): Unit = {
        addLocalModification(SET, i, collection.update(i, t))
    }

    def remove(i: Int): Unit = {
        addLocalModification(REMOVE, i, collection.remove(i))
    }

    private def addLocalModification(@NotNull kind: CollectionModification, @Nullable index: Int, value: Any): Unit = {
        if (autoFlush) {
            flushModification((kind, index, value))
            return
        }

        kind match {
            case CLEAR => collectionModifications.clear()
            case SET | REMOVE => collectionModifications.filterInPlace(m => !(m._1 == SET && m._2 == index))
            case ADD => //Do not optimise : the addition may be different according to the order
        }
        collectionModifications += ((kind, index, value))
    }

    private def flushModification(mod: (CollectionModification, Int, Any), targetID: String = "BROADCAST"): Unit = {
        collector.sendPacket(ObjectPacket(mod), targetID)
        networkListeners.applyAll(mod.asInstanceOf[(CollectionModification, Int, T)])
        modCount += 1
    }

    def clear(): Unit = {
        collection.clear()
        addLocalModification(CLEAR, -1, null)
    }

    /**
     * (CollectionModification, _, _) => the kind of modification that were done
     * (_, Int, _) => the index affected (may be -1 for mod kinds that does not specify any index such as CLEAR)
     * (_, _, T) => The object affected (may be null for mod kinds that does not specify any object such as CLEAR, or REMOVE)
     * */
    def addListener(action: (CollectionModification, Int, T) => Unit): Unit = {
        networkListeners += (tuple3 => action(tuple3._1, tuple3._2, tuple3._3))
    }

    private def start(): Unit = {
        collector.broadcastPacket(DataPacket("Init"))

        collector.addOnPacketInjected((packet, coords) => {
            packet match {
                case modPacket: ObjectPacket => handleNetworkModRequest(modPacket)

                case DataPacket(order, _) if order == "Init" =>
                    val array: Array[Any] = collection.toArray
                    collector.sendPacket(WrappedPacket("InitBack", ObjectPacket(array)), coords.senderID)

                case WrappedPacket("InitBack", ObjectPacket(array: Array[T])) => if (uninitialised) {
                    collection.addAll(array)
                    uninitialised = false
                }
            }
        })
    }

    private def handleNetworkModRequest(packet: ObjectPacket): Unit = {
        val mod: (CollectionModification, Int, Any) = packet.casted

        val modKind = mod._1
        val index = mod._2
        lazy val item = mod._3.asInstanceOf[T] //Only instantiate value if needed

        modKind match {
            case CLEAR => collection.clear()
            case SET => collection.update(index, item)
            case REMOVE => collection.remove(index)
            case ADD => if (index < 0) collection.addOne(item) else collection.insert(index, item)
        }

        networkListeners.applyAll(mod.asInstanceOf[(CollectionModification, Int, T)])
        println("Collection is now " + collection)
    }

}
