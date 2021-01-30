package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.JustifiedCloseable

trait PacketInjectable extends JustifiedCloseable {
    val identifier: Int
    val ownerID: String
    val injector: PacketTraffic

    def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit
}