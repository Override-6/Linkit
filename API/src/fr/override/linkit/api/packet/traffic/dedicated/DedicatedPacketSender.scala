package fr.`override`.linkit.api.packet.traffic.dedicated

import fr.`override`.linkit.api.packet.Packet

trait DedicatedPacketSender {

    def sendPacket(packet: Packet): Unit

}
