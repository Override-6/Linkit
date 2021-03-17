package fr.`override`.linkit.server

import fr.`override`.linkit.skull.connection.packet.traffic.{PacketTraffic, PacketWriter}
import fr.`override`.linkit.skull.connection.packet.Packet
import fr.`override`.linkit.skull.internal.system.event.packet.PacketEvents
import fr.`override`.linkit.server.exceptions.ConnectionException

class ServerPacketWriter(server: RelayServer, info: WriterInfo) extends PacketWriter {

    override val identifier: Int = info.identifier
    override val traffic: PacketTraffic = info.traffic
    override val relayID: String = traffic.relayID
    override val ownerID: String = traffic.ownerID

    private val notifier = info.notifier
    private val hooks = info.packetHooks

    override def writePacket(packet: Packet, targetIDs: String*): Unit = {
        targetIDs.foreach(targetID => {
            if (targetID == server.identifier) {
                val coords = DedicatedPacketCoordinates(identifier, targetID, relayID)
                val event = PacketEvents.packedSent(packet, coords)
                notifier.notifyEvent(hooks, event)
                traffic.handleInjection(PacketInjections.unhandled(coords, packet))
                return
            }
            if (server.isConnected(targetID)) {
                server.getConnection(targetID).get.sendPacket(packet, identifier)

            } else {
                throw ConnectionException(s"Attempted to send a packet to target $targetID, but this target is not connected.")
            }
        })
    }

    override def writeBroadcastPacket(packet: Packet, discarded: String*): Unit = {
        server.broadcastPacketToConnections(packet, ownerID, identifier, discarded: _*)
    }
}
