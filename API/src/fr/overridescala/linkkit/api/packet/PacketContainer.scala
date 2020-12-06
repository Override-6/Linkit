package fr.overridescala.linkkit.api.packet

import fr.overridescala.linkkit.api.system.JustifiedCloseable

trait PacketContainer extends JustifiedCloseable {
    val identifier: Int

    def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit

}
