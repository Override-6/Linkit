package fr.`override`.linkit.api.packet.traffic.global

import fr.`override`.linkit.api.packet.Packet

trait GlobalPacketSender {
    def sendPacket(packet: Packet, targetID: String): Unit

    def broadcastPacket(packet: Packet): Unit = sendPacket(packet, "BROADCAST")
}
