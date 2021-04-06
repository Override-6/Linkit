package fr.linkit.core.connection.packet.serialization

import fr.linkit.api.connection.packet.serialization.{Serializer, TransferInfo}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.packet.fundamental.EmptyPacket

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

case class SimpleTransferInfo(override val coords: PacketCoordinates,
                              override val attributes: PacketAttributes,
                              override val packet: Packet) extends TransferInfo {

    override def makeSerial(serializer: Serializer): Array[Byte] = {
        val buff = ArrayBuffer.empty[Serializable]
        buff += coords
        if (attributes.nonEmpty)
            buff += attributes
        if (packet != EmptyPacket)
            buff += packet
        AppLogger.debug(s"buff = $buff")
        serializer.serialize(buff.toArray, true)
    }
}
