package fr.`override`.linkit.api.utils.cache

import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.packet.collector.AsyncPacketCollector

class SharedInstance[A <: Serializable, B <: Serializable](collector: AsyncPacketCollector, mapper: A => B) extends SharedCache {

    override var autoFlush: Boolean = true
    @volatile private var instance: B = _
    @volatile private var modCount = 0

    collector.addOnPacketInjected((packet, _) => {
        packet match {
            case ObjectPacket(remoteInstance: B) =>
                this.instance = remoteInstance
                modCount += 1

                println(s"instance has been remotely updated ($instance)")
                println(s"modCount = ${modCount}")

            case _ => throw new UnexpectedPacketException("Unable to handle a non-ObjectPacket into SharedInstance")
        }
    })

    override def modificationCount(): Int = modCount

    def get: B = instance

    def set(t: B): Unit = {
        instance = t
        modCount += 1
        if (autoFlush)
            flush()
    }

    override def flush(): Unit = {
        collector.broadcastPacket(ObjectPacket(instance))
    }
}