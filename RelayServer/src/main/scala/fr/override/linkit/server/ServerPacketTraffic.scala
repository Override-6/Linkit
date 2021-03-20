package fr.`override`.linkit.server

import fr.`override`.linkit.api.connection.packet.Packet
import fr.`override`.linkit.api.connection.packet.traffic.PacketWriter
import fr.`override`.linkit.core.connection.packet.traffic.{AbstractPacketTraffic, WriterInfo}

class ServerPacketTraffic(serverContext: ServerApplicationContext) extends AbstractPacketTraffic(server.identifier) {
    override val ownerID: String = serverContext.identifier

    override def newWriter(identifier: Int, transform: Packet => Packet): PacketWriter = {
        new ServerPacketWriter(serverContext, WriterInfo(this, identifier, transform))
    }
}
