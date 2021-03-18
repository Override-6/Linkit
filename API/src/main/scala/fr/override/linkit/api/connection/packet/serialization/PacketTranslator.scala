package fr.`override`.linkit.api.connection.packet.serialization

import fr.`override`.linkit.api.connection.packet.{Packet, PacketCoordinates}

trait PacketTranslator {

    def translate(packet: Packet, coordinates: PacketCoordinates): PacketSerializationResult

    def translate(bytes: Array[Byte]): (Packet, PacketCoordinates)

}
