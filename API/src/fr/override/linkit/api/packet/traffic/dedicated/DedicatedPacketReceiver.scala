package fr.`override`.linkit.api.packet.traffic.dedicated

import fr.`override`.linkit.api.packet.{Packet, PacketCompanion}

trait DedicatedPacketAsyncReceiver extends PacketChannel {
    def onPacketReceived(callback: Packet => Unit): Unit
}

trait DedicatedPacketSyncReceiver extends PacketChannel {

    def nextPacket(): Packet

    def nextPacketAsP[P <: Packet](): P = nextPacket().asInstanceOf[P]

    def nextPacket[P <: Packet](classOfP: Class[P]): P = nextPacketAsP()

    def nextPacket[P <: Packet](factoryOfP: PacketCompanion[P]): P = nextPacket(factoryOfP.packetClass)

    /**
     * @return true if this channel contains stored packets. In other words, return true if [[nextPacket]] will not wait
     * */
    def haveMorePackets: Boolean
}