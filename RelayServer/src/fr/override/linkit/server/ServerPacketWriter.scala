package fr.`override`.linkit.server

import fr.`override`.linkit.api.packet.traffic.{PacketTraffic, PacketWriter, WriterInfo}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}

class ServerPacketWriter(server: RelayServer, info: WriterInfo) extends PacketWriter {
    override val identifier: Int = info.identifier
    override val traffic: PacketTraffic = info.traffic
    override val relayID: String = traffic.relayID

    override def writePacket(packet: Packet, targetID: String): Unit = {
        if (targetID == "BROADCAST") {
            writeBroadcastPacket(packet)
            return
        }
        if (targetID == server.identifier) {
            traffic.injectPacket(packet, PacketCoordinates(identifier, targetID, relayID))
            return
        }
        if (server.isConnected(targetID))
            server.getConnection(targetID).sendPacket(packet, identifier)
    }

    override def writeBroadcastPacket(packet: Packet): Unit = {
        server.broadcastPacket(packet, identifier)
    }
}
