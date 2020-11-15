package fr.overridescala.vps.ftp.api.packet

import fr.overridescala.vps.ftp.api.`extension`.event.EventDispatcher.EventNotifier
import fr.overridescala.vps.ftp.api.`extension`.packet.PacketManager
import fr.overridescala.vps.ftp.api.system.Reason

import scala.collection.mutable

class PacketChannelsHandler(val notifier: EventNotifier,
                            socket: DynamicSocket,
                            packetManager: PacketManager) {

    private val openedPacketChannels = mutable.Map.empty[Int, PacketChannelManager]

    def registerManager(packetChannel: PacketChannelManager): Unit = {
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

    def unregisterManager(id: Int, reason: Reason): Unit = {
        val opt = openedPacketChannels.remove(id)
        if (opt.isDefined)
            notifier.onPacketChannelUnregistered(opt.get, reason)
    }

    def sendPacket(packet: Packet): Unit = {
        socket.write(packetManager.toBytes(packet))
        notifier.onPacketSent(packet)
    }

    def notifyPacketUsed(packet: Packet): Unit = notifier.onPacketUsed(packet)

}