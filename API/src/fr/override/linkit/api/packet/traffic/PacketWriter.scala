package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}

trait PacketWriter {

    val ownerID: String

    def writePacket(packet: Packet, coords: PacketCoordinates): Unit

    def writePacket(packet: Packet, identifier: Int, targetID: String): Unit

}
