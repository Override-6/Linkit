package fr.`override`.linkit.api.utils.cache

import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.packet.collector.AsyncPacketCollector

class SharedInstance[T <: Serializable](collector: AsyncPacketCollector, autoFlush: Boolean) extends SharedCache {

    override val isAutoFlush: Boolean = autoFlush
    @volatile private var instance: T = _

    collector.addOnPacketInjected((packet, _) => {
        packet match {
            case objectPacket: ObjectPacket =>
                instance = objectPacket.obj.asInstanceOf[T]
                modCount += 1

                println(s"instance has been remotely updated ($instance)")
                println(s"modCount = ${modCount}")

            case _ => throw new UnexpectedPacketException("Unable to handle a non-ObjectPacket into SharedInstance")
        }
    })
    @volatile private var modCount = 0

    override def modificationCount(): Int = modCount

    def get: T = instance

    def set(t: T): Unit = {
        instance = t
        modCount += 1
        println(s"modCount = ${modCount}")
        if (autoFlush)
            flush()
    }

    override def flush(): Unit = {
        collector.broadcastPacket(ObjectPacket(instance))
    }
}
