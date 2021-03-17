package fr.`override`.linkit.server

import fr.`override`.linkit.skull.connection.packet.Packet
import fr.`override`.linkit.skull.connection.packet.traffic.PacketWriter

class ServerPacketTraffic(server: RelayServer) extends AbstractPacketTraffic(server.configuration, server.identifier) {
    override val ownerID: String = server.identifier

    override def newWriter(identifier: Int, transform: Packet => Packet): PacketWriter = {
        new ServerPacketWriter(server, WriterInfo(this, identifier, transform))
    }
}
