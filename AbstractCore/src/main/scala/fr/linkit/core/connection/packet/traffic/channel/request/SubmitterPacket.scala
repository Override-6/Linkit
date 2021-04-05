package fr.linkit.core.connection.packet.traffic.channel.request

import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketAttributesPresence}
import fr.linkit.core.local.utils.ScalaUtils.ensureType

import java.util.NoSuchElementException
import scala.reflect.ClassTag

sealed abstract class SubmitterPacket(packets: Array[Packet]) extends Packet with PacketAttributes {

    @transient private var packetIndex                  = 0
    @transient private var attributes: PacketAttributes = _

    @throws[NoSuchElementException]("If this method is called more times than packet array's length" + this)
    def nextPacket[P <: Packet : ClassTag]: P = {
        if (packetIndex >= packets.length)
            throw new NoSuchElementException()

        val packet = packets(packetIndex)
        packetIndex += 1
        ensureType[P](packet)
    }

    def foreach(action: Packet => Unit): Unit = {
        packets.foreach(action)
    }

    def getAttributes: PacketAttributes = attributes

    override def getAttribute[S <: Serializable](key: Serializable): Option[S] = attributes.getAttribute(key)

    override def getPresence[S <: Serializable](presence: PacketAttributesPresence): Option[S] = attributes.getPresence(presence)

    override def putAttribute(key: Serializable, value: Serializable): this.type = {
        attributes.putAttribute(key, value)
        this
    }

    override def putPresence(presence: PacketAttributesPresence, value: Serializable): this.type = {
        attributes.putPresence(presence, value)
        this
    }

    override def drainAttributes(other: PacketAttributes): this.type = {
        attributes.drainAttributes(other)
        this
    }

    override def isEmpty: Boolean = attributes.isEmpty

    private[packet] def setAttributes(attributes: PacketAttributes): Unit = {
        if (this.attributes != null)
            throw new IllegalStateException("Attributes already set !")
        this.attributes = attributes
    }

}

case class ResponsePacket(id: Long, packets: Array[Packet])
        extends SubmitterPacket(packets) {

}

case class RequestPacket(id: Long, packets: Array[Packet])
        extends SubmitterPacket(packets)
