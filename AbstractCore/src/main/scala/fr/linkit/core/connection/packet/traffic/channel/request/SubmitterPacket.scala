package fr.linkit.core.connection.packet.traffic.channel.request

import fr.linkit.api.connection.packet.{Packet, PacketAttributes}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.packet.AbstractAttributesPresence
import fr.linkit.core.local.utils.ScalaUtils.ensurePacketType

import java.util.NoSuchElementException
import scala.reflect.ClassTag

sealed abstract class SubmitterPacket(id: Long, packets: Array[Packet]) extends AbstractAttributesPresence with Packet {

    @transient private var packetIndex                  = 0
    @transient private var attributes: PacketAttributes = _

    @throws[NoSuchElementException]("If this method is called more times than packet array's length" + this)
    def nextPacket[P <: Packet : ClassTag]: P = {
        AppLogger.debug(s"packetIndex: $packetIndex, packets: ${packets.mkString("Array(", ", ", ")")}")
//        Thread.dumpStack()
        if (packetIndex >= packets.length)
            throw new NoSuchElementException()

        val packet = packets(packetIndex)
        packetIndex += 1
        ensurePacketType[P](packet)
    }

    def foreach(action: Packet => Unit): Unit = {
        packets.foreach(action)
    }

    def getAttributes: PacketAttributes = attributes

    def getAttribute[S](key: Serializable): Option[S] = attributes.getAttribute(key)

    def putAttribute(key: Serializable, value: Serializable): this.type = {
        attributes.putAttribute(key, value)
        this
    }

    private[packet] def setAttributes(attributes: PacketAttributes): Unit = {
        if (this.attributes != null && this.attributes.ne(attributes))
            throw new IllegalStateException("Attributes already set !")
        AppLogger.debug(s"SETTING ATTRIBUTES FOR SUBMITTER PACKET $this : $attributes")
        this.attributes = attributes
    }

    override def toString: String = s"${getClass.getSimpleName}(id: $id, packets: ${packets.mkString("Array(", ", ", ")")}, attr: $attributes)"

}

case class ResponsePacket(id: Long, packets: Array[Packet])
        extends SubmitterPacket(id, packets) {

}

case class RequestPacket(id: Long, packets: Array[Packet])
        extends SubmitterPacket(id, packets)
