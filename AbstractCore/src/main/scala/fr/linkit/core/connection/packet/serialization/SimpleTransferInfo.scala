package fr.linkit.core.connection.packet.serialization

import fr.linkit.api.connection.packet.serialization.{Serializer, TransferInfo}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.core.connection.packet.EmptyPacketAttributes
import fr.linkit.core.connection.packet.fundamental.EmptyPacket

import scala.collection.mutable.ListBuffer

case class SimpleTransferInfo(override val coords: PacketCoordinates,
                              override val attributes: PacketAttributes,
                              override val packet: Packet) extends TransferInfo {

    override def makeSerial(serializer: Serializer): Array[Byte] = {
        val buff = ListBuffer.empty[Serializable]
        buff += coords
        if (attributes != EmptyPacketAttributes)
            buff += attributes
        if (packet != EmptyPacket)
            buff += packet
        serializer.serialize(buff.toArray, true)
    }
}
