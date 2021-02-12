package fr.`override`.linkit.server

import fr.`override`.linkit.api.packet.Packet
import fr.`override`.linkit.api.packet.traffic.{AbstractPacketTraffic, PacketWriter, WriterInfo}

class ServerPacketTraffic(server: RelayServer) extends AbstractPacketTraffic(server.configuration, server.identifier) {
    override val ownerID: String = server.identifier

    override def newWriter(identifier: Int, transform: Packet => Packet): PacketWriter = {
        new ServerPacketWriter(server, WriterInfo(this, identifier, transform))
    }
}
