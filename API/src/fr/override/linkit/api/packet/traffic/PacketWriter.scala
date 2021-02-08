package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.packet.Packet

trait PacketWriter {

    val relayID: String
    val identifier: Int
    val traffic: PacketTraffic

    def writePacket(packet: Packet, targetID: String): Unit

    def writeBroadcastPacket(packet: Packet, discarded: Array[String] = Array()): Unit

}
