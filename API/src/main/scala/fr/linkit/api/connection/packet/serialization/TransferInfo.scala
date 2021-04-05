package fr.linkit.api.connection.packet.serialization

import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}

trait TransferInfo {

    val coords: PacketCoordinates

    val attributes: PacketAttributes

    val packet: Packet

    def makeSerial(serializer: Serializer): Array[Byte]

}
