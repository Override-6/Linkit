package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}

trait PacketSender {

    val ownerID: String

    def sendPacket(packet: Packet, coords: PacketCoordinates): Unit

    def sendPacket(packet: Packet, identifier: Int, targetID: String): Unit

    def checkThread(): Unit //FIXME find another place to move this method, this method must not be declared in a PacketSender
}
