package fr.overridescala.vps.ftp.api.packet

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class PacketChannelManagerCache {

    private val openedPacketChannels = mutable.Map.empty[Int, PacketChannelManager]

    private val lostPackets = mutable.Map.empty[Int,ListBuffer[Packet]]

    def registerPacketChannel(packetChannel: PacketChannelManager): Unit = {
        val id = packetChannel.channelID
        if (openedPacketChannels.contains(id))
            throw new IllegalArgumentException(s"A packet channel with id '$id' is already registered to this channel list !")
        if (lostPackets.contains(id))
            lostPackets()
        openedPacketChannels.put(id, packetChannel)
    }

    def injectPacket(packet: Packet): Unit = {
        openedPacketChannels(packet.channelID)
                .addPacket(packet)
    }

    def unregisterPaketChannel(id: Int): Unit =
        openedPacketChannels.remove(id)

}
