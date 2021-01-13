package fr.`override`.linkit.server

import fr.`override`.linkit.api.packet.traffic.AbstractPacketTraffic
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}

class GlobalPacketTraffic(server: RelayServer) extends AbstractPacketTraffic(server, server.identifier) {

    override def send(packet: Packet, coords: PacketCoordinates): Unit = {
        if (coords.targetID == "BROADCAST") {
            server.broadcastPacket(packet, coords.injectableID)
            return
        }
        if (server.isConnected(coords.targetID))
            server.getConnection(coords.targetID).sendPacket(packet, coords.injectableID)
    }

}
