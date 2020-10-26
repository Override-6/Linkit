package fr.overridescala.vps.ftp.api.packet

import scala.collection.mutable

class PacketChannelManagerCache {

    private val openedPacketChannels = mutable.Map.empty[Int, PacketChannelManager]

    def registerPacketChannel(packetChannel: PacketChannelManager): Unit = {
        val id = packetChannel.channelID
        if (openedPacketChannels.contains(id))
            throw new IllegalArgumentException(s"A packet channel with id '$id' is already registered to this channel list !")
        //FIXME
        openedPacketChannels.put(id, packetChannel)

    }

    def injectPacket(packet: Packet): Unit = {
        val channelID = packet.channelID
        openedPacketChannels(channelID)
                .addPacket(packet)
    }

    def unregisterPaketChannel(id: Int): Unit = {
        openedPacketChannels.remove(id)
    }

}
