package fr.`override`.linkit.api.utils.cache

import fr.`override`.linkit.api.packet.collector.AsyncPacketCollector
import fr.`override`.linkit.api.utils.ConsumerContainer
import fr.`override`.linkit.api.utils.cache.SharedCollection.{Add, Clear, Init, InitBack, Remove, Set}
import org.jetbrains.annotations.{NotNull, Nullable}

import scala.collection.mutable.ListBuffer

class SharedCollection[T](collector: AsyncPacketCollector, autoFlush: Boolean) extends SharedCache {

    override val isAutoFlush: Boolean = autoFlush
    private val collection = ListBuffer.empty[T]
    private val collectionModifications = ListBuffer.empty[(Int, Int, Any)]
    private val networkListeners = ConsumerContainer[(Int, Int, T)]()
    @volatile private var modCount = 0

    flushModification((Init, -1, null))

    collector.addOnPacketInjected((packet, coords) => {
        val mod: (Int, Int, Any) = packet.asInstanceOf[ObjectPacket].casted
        val orderKind = mod._1
        val obj = mod._3
        orderKind match {
            case Init =>
                val array: Array[Any] = collection.toArray
                collector.sendPacket(ObjectPacket((InitBack, null, array)), coords.senderID)
            case InitBack => if (uninitialised) {
                collection.addAll(obj.asInstanceOf[Array[T]])
                uninitialised = false
            }
            case _ => handleNetworkModification(mod)
        }
        networkListeners.applyAll(mod.asInstanceOf[(Int, Int, T)])
        println("Collection is now " + collection)
    })
    @volatile private var uninitialised = true

    override def flush(): Unit = this.synchronized {
        collectionModifications.foreach(flushModification(_))
        collectionModifications.clear()
    }

    override def modificationCount(): Int = modCount

    override def toString: String = collection.toString()

    def add(t: T): Unit = {
        collection += t
        addLocalModification(Add, -1, t)
    }

    def add(i: Int, t: T): Unit = {
        collection.insert(i, t)
        addLocalModification(Add, i, t)
    }

    private def addLocalModification(@NotNull kind: Int, @Nullable index: Int, value: Any): Unit = {
        if (autoFlush) {
            flushModification((kind, index, value))
            return
        }

        kind match {
            case Clear => collectionModifications.clear()
            case Set | Remove => collectionModifications.filterInPlace(m => !(m._1 == Set && m._2 == index))
            case Add => //Do not optimise : the addition may be different according to the order
        }
        collectionModifications += ((kind, index, value))
    }

    private def flushModification(mod: (Int, Int, Any), targetID: String = "BROADCAST"): Unit = {
        collector.sendPacket(ObjectPacket(mod), targetID)
        networkListeners.applyAll(mod.asInstanceOf[(Int, Int, T)])
        modCount += 1
    }

    def set(i: Int, t: T): Unit = {
        collection.update(i, t)
        addLocalModification(Set, i, t)
    }

    def remove(i: Int): Unit = {
        collection.remove(i)
        addLocalModification(Remove, i, null)
    }

    def clear(): Unit = {
        collection.clear()
        addLocalModification(Clear, -1, null)
    }

    def addListener(action: (Int, Int, T) => Unit): Unit = {
        networkListeners += (tuple3 => action(tuple3._1, tuple3._2, tuple3._3))
    }

    private def handleNetworkModification(mod: (Int, Int, Any)): Unit = {
        val modKind = mod._1
        val index = mod._2
        lazy val item = mod._3.asInstanceOf[T] //Only instantiate value if needed

        modKind match {
            case Clear => collection.clear()
            case Set => collection.update(index, item)
            case Remove => collection.remove(index)
            case Add => if (index < 0) collection.addOne(item) else collection.insert(index, item)
        }
    }

}

object SharedCollection {
    val Set = 1
    val Clear = 2
    val Remove = 3
    val Add = 4
    val Init = 5
    val InitBack = 6
}
