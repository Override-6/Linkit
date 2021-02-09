package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.packet.{Packet, PacketCompanion, PacketCoordinates}

trait PacketAsyncReceiver extends PacketChannel {
    def addOnPacketReceived(callback: (Packet, PacketCoordinates) => Unit): Unit
}

trait PacketSyncReceiver extends PacketChannel {

    def nextPacket(): Packet

    def nextPacketAsP[P <: Packet](): P = nextPacket().asInstanceOf[P]

    def nextPacket[P <: Packet](classOfP: Class[P]): P = nextPacketAsP()

    def nextPacket[P <: Packet](factoryOfP: PacketCompanion[P]): P = nextPacketAsP()

    /**
     * @return true if this channel contains stored packets. In other words, return true if [[nextPacket]] will not wait
     * */
    def haveMorePackets: Boolean
}