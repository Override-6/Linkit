package fr.`override`.linkit.api.packet

import fr.`override`.linkit.api.system.JustifiedCloseable

trait PacketContainer extends JustifiedCloseable {
    val identifier: Int

    def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit

}
