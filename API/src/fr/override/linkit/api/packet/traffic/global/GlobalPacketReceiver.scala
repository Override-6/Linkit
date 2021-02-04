package fr.`override`.linkit.api.packet.traffic.global

import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}

trait GlobalPacketAsyncReceiver {
    def addOnPacketReceived(callback: (Packet, PacketCoordinates) => Unit): Unit
}

trait GlobalPacketSyncReceiver {

    def nextPacket[P <: Packet](typeOfP: Class[P]): P = nextPacketAndCoordinates(typeOfP)._1

    def nextPacketAndCoordinates[P <: Packet](typeOfP: Class[P]): (P, PacketCoordinates)

    def haveMorePackets: Boolean
}