package fr.`override`.linkit.api.connection.packet.serialization

import fr.`override`.linkit.api.connection.packet.{Packet, PacketCoordinates}

case class PacketSerializationResult(packet: Packet, coords: PacketCoordinates, serializer: Serializer, bytes: Array[Byte]) {

    def writableBytes(): Array[Byte] = {
        val length = bytes.length
        Array[Byte](
            ((length >> 24) & 0xff).toByte,
            ((length >> 16) & 0xff).toByte,
            ((length >> 8) & 0xff).toByte,
            ((length >> 0) & 0xff).toByte
        ) ++ bytes
    }

}
