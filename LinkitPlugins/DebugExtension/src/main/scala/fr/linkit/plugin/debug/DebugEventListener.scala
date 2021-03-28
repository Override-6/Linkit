package fr.linkit.plugin.debug

import fr.linkit.api.connection.packet.{Packet, PacketCoordinates}

class DebugEventListener  {

    def onPacketReceived(packet: Packet, coordinates: PacketCoordinates): Unit = {
        println(s"Received packet : '$packet', with coordinates $coordinates")
    }

    def onPacketSent(packet: Packet, coordinates: PacketCoordinates): Unit = {
        println(s"Sent packet : '$packet', to coordinates $coordinates")
    }

}
