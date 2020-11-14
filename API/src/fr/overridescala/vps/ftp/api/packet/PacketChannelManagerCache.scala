package fr.overridescala.vps.ftp.api.packet

import fr.overridescala.vps.ftp.api.Reason
import fr.overridescala.vps.ftp.api.`extension`.event.EventDispatcher.EventNotifier

import scala.collection.mutable

class PacketChannelManagerCache(private[packet] val notifier: EventNotifier) { //Notifier is accessible from api.packet to reduce parameter number in (A)SyncPacketChannel

    private val openedPacketChannels = mutable.Map.empty[Int, PacketChannelManager]

    def registerPacketChannel(packetChannel: PacketChannelManager): Unit = {
        val id = packetChannel.channelID
        if (openedPacketChannels.contains(id))
            throw new IllegalArgumentException(s"A packet channel with id '$id' is already registered to this channel list !")
        openedPacketChannels.put(id, packetChannel)
        notifier.onPacketChannelRegistered(packetChannel)
    }

    def injectPacket(packet: Packet): Unit = {
        val channelID = packet.channelID
        openedPacketChannels(channelID)
                .addPacket(packet)
    }

    def unregisterPaketChannel(id: Int, reason: Reason): Unit = {
        val opt = openedPacketChannels.remove(id)
        if (opt.isDefined)
            notifier.onPacketChannelUnregistered(opt.get, reason)
    }

}