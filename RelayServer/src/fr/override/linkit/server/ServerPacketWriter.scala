package fr.`override`.linkit.server

import fr.`override`.linkit.api.packet.traffic.{PacketInjections, PacketTraffic, PacketWriter, WriterInfo}
import fr.`override`.linkit.api.packet.{DedicatedPacketCoordinates, Packet}
import fr.`override`.linkit.server.exceptions.ConnectionException

class ServerPacketWriter(server: RelayServer, info: WriterInfo) extends PacketWriter {

    override val identifier: Int = info.identifier
    override val traffic: PacketTraffic = info.traffic
    override val relayID: String = traffic.relayID
    override val ownerID: String = traffic.ownerID

    override def writePacket(packet: Packet, targetIDs: String*): Unit = targetIDs.foreach(targetID => {
        if (targetID == server.identifier) {
            traffic.handleInjection(PacketInjections.unhandled(DedicatedPacketCoordinates(identifier, targetID, relayID), packet))
            return
        }
        println(s"WRITING PACKETS $packet TO $targetID")
        if (server.isConnected(targetID)) {
            server.getConnection(targetID).sendPacket(packet, identifier)
        } else {
            throw ConnectionException(s"Attempted to send a packet to target $targetID, but this target is not connected.")
        }
    })

    override def writeBroadcastPacket(packet: Packet, discarded: String*): Unit = {
        server.broadcastPacketToConnections(packet, ownerID, identifier, discarded: _*)
    }
}
