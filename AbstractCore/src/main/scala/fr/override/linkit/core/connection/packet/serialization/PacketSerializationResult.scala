package fr.`override`.linkit.core.connection.packet.serialization

import fr.`override`.linkit.skull.connection.packet.{Packet, PacketCoordinates}

case class PacketSerializationResult(packet: Packet, coords: PacketCoordinates, serializer: ObjectSerializer, bytes: Array[Byte]) {

    def writableBytes(): Array[Byte] = NumberSerializer.serializeInt(bytes.length) ++ bytes

}
