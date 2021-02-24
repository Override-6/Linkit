package fr.`override`.linkit.api.network.cache

import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.packet.fundamental.RefPacket.ObjectPacket
import fr.`override`.linkit.api.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.ConsumerContainer

class SharedInstance[A <: Serializable] private(family: String,
                                                identifier: Long,
                                                channel: CommunicationPacketChannel) extends HandleableSharedCache(family, identifier, channel) {

    override var autoFlush: Boolean = true

    private val listeners = new ConsumerContainer[A]

    @volatile private var modCount = 0
    @volatile private var instance: A = _

    def this(family: String,
             identifier: Long,
             channel: CommunicationPacketChannel,
             value: A = null) = {
        this(family, identifier, channel)
        instance = value
    }

    override def handlePacket(packet: Packet, coords: PacketCoordinates): Unit = {
        //println(s"<$family> Handling packet $packet")
        packet match {
            case ObjectPacket(remoteInstance: A) =>
                this.instance = remoteInstance
                modCount += 1
                listeners.applyAll(remoteInstance)
                //println(s"<$family> INSTANCE IS NOW (network): $instance")

            case _ => throw new UnexpectedPacketException("Unable to handle a non-ObjectPacket into SharedInstance")
        }
    }

    override def modificationCount(): Int = modCount

    def get: A = instance

    def set(t: A): this.type = {
        instance = t
        modCount += 1
        listeners.applyAll(t)
        //println(s"INSTANCE IS NOW (local) : $instance $autoFlush")
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
        (family: String, identifier: Long, baseContent: Array[Any], channel: CommunicationPacketChannel) => {
            if (baseContent.isEmpty)
                new SharedInstance[A](family, identifier, channel)
            else new SharedInstance[A](family, identifier, channel, baseContent(0).asInstanceOf[A])
        }
    }

}