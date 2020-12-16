package fr.`override`.linkkit.api.packet

import fr.`override`.linkkit.api.system.JustifiedCloseable

trait PacketContainer extends JustifiedCloseable {
    val identifier: Int

    def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit

}
