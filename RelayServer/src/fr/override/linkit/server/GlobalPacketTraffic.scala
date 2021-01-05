package fr.`override`.linkit.server

import fr.`override`.linkit.api.packet.traffic.AbstractPacketTraffic
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}

class GlobalPacketTraffic(server: RelayServer) extends AbstractPacketTraffic(server, server.identifier) {

    override def sendPacket(packet: Packet, coords: PacketCoordinates): Unit = {
        server.getConnection(coords.targetID).sendPacket(packet, coords.injectableID)
    }
    
}
