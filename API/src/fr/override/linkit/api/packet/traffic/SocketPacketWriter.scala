package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.packet.serialization.PacketTranslator
import fr.`override`.linkit.api.packet.{BroadcastPacketCoordinates, DedicatedPacketCoordinates, Packet}

class SocketPacketWriter(socket: DynamicSocket,
                         translator: PacketTranslator,
                         info: WriterInfo) extends PacketWriter {

    override val traffic: PacketTraffic = info.traffic
    override val relayID: String = traffic.relayID
    override val ownerID: String = traffic.ownerID
    override val identifier: Int = info.identifier

    override def writePacket(packet: Packet, targetIDs: String*): Unit = {
        val transformedPacket = info.transform(packet)

        val coords = if (targetIDs.length == 1) {
            val target = targetIDs.head
            val dedicated = DedicatedPacketCoordinates(identifier, targetIDs(0), ownerID)
            if (target == relayID) {
                traffic.handleInjection(PacketInjections.unhandled(dedicated, packet))
                return
            }
            dedicated
        } else {
            if (targetIDs.contains(relayID))
                traffic.handleInjection(PacketInjections.unhandled(DedicatedPacketCoordinates(identifier, relayID, ownerID), packet))

            BroadcastPacketCoordinates(identifier, ownerID, false, targetIDs.filter(_ != relayID): _*)
        }

        //println(s"WRITING COORDS & PACKETS ($coords, $transformedPacket)")
        socket.write(translator.fromPacketAndCoords(transformedPacket, coords))
    }

    override def writeBroadcastPacket(packet: Packet, discardedIDs: String*): Unit = {
        val transformedPacket = info.transform(packet)
        val coords = BroadcastPacketCoordinates(identifier, ownerID, true, discardedIDs: _*)

        socket.write(translator.fromPacketAndCoords(transformedPacket, coords))
    }

}
