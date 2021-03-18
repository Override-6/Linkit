package fr.`override`.linkit.api.connection.packet.traffic

import fr.`override`.linkit.api.connection.packet.Packet

trait PacketWriter {

    val relayID: String
    val ownerID: String
    val identifier: Int
    val traffic: PacketTraffic

    def writePacket(packet: Packet, targetIDs: String*): Unit

    def writeBroadcastPacket(packet: Packet, discardedIDs: String*): Unit

}
