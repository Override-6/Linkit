package fr.`override`.linkit.api.network.cache

import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.ConsumerContainer

class SharedInstance[A <: Serializable](family: String,
                                        identifier: Int,
                                        channel: CommunicationPacketChannel) extends HandleableSharedCache(family, identifier, channel) {

    override var autoFlush: Boolean = true

    private val listeners = new ConsumerContainer[A]
    @volatile private var modCount = 0
    @volatile private var instance: A = _

    override def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = {
        packet match {
            case ObjectPacket(remoteInstance: A) =>
                this.instance = remoteInstance
                modCount += 1
                listeners.applyAll(remoteInstance)

            case _ => throw new UnexpectedPacketException("Unable to handle a non-ObjectPacket into SharedInstance")
        }
    }

    override def modificationCount(): Int = modCount

    def get: A = instance

    def set(t: A): this.type = {
        instance = t
        modCount += 1
        listeners.applyAll(t)
        if (autoFlush)
            flush()
        this
    }

    override def flush(): this.type = {
        sendRequest(ObjectPacket(instance))
        this
    }

    def addListener(callback: A => Unit): this.type = {
        listeners += callback
        this
    }

    override def currentContent: Array[Any] = Array(instance)
}

object SharedInstance {

    def apply[A <: Serializable]: SharedCacheFactory[SharedInstance[A]] = {
        new SharedCacheFactory[SharedInstance[A]] {
            override def createNew(family: String, identifier: Int, baseContent: Array[AnyRef], channel: CommunicationPacketChannel): SharedInstance[A] = {
                new SharedInstance[A](family, identifier, channel)
            }

            override def sharedCacheClass: Class[SharedInstance[A]] = classOf[SharedInstance[A]]
        }
    }

}