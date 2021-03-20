package fr.`override`.linkit.api.local.system.config

import fr.`override`.linkit.api.connection.packet.{Packet, PacketCoordinates}

trait Checker {

    def checkConnectionCount(count: Int): Unit

    def checkPacket(packet: Packet, coordinates: PacketCoordinates): Unit

}
