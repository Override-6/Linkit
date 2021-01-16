package fr.`override`.linkit.api.utils.cache

import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.packet.collector.CommunicationPacketCollector
import fr.`override`.linkit.api.utils.ConsumerContainer

class SharedInstance[A <: Serializable](collector: CommunicationPacketCollector) extends SharedCache {

    override var autoFlush: Boolean = true

    private val listeners = new ConsumerContainer[A]
    @volatile private var modCount = 0
    @volatile private var instance: A = _

    collector.addRequestListener((packet, _) => {
        packet match {
            case ObjectPacket(remoteInstance: A) =>
                this.instance = remoteInstance
                modCount += 1

                println(s"instance has been remotely updated ($instance)")
                println(s"modCount = ${modCount}")

            case _ => throw new UnexpectedPacketException("Unable to handle a non-ObjectPacket into SharedInstance")
        }
    })

    override def modificationCount(): Int = modCount

    def get: A = instance

    def set(t: A): Unit = {
        instance = t
        modCount += 1
        if (autoFlush)
            flush()
    }

    override def flush(): this.type = {
        collector.broadcastPacket(ObjectPacket(instance))
        this
    }

    def addListener(callback: A => Unit): Unit = {
        listeners += callback
    }

}