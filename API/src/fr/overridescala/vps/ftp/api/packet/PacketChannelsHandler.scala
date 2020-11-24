package fr.overridescala.vps.ftp.api.packet

import fr.overridescala.vps.ftp.api.system.event.EventDispatcher.EventNotifier
import fr.overridescala.vps.ftp.api.system.{JustifiedCloseable, Reason}

import scala.collection.mutable

class PacketChannelsHandler(val notifier: EventNotifier,
                            socket: DynamicSocket,
                            packetManager: PacketManager) extends JustifiedCloseable {

    private val openedPacketChannels = mutable.Map.empty[Int, PacketChannelManager]

    def registerManager(packetChannel: PacketChannelManager): Unit = {
        val id = packetChannel.channelID
        if (openedPacketChannels.contains(id))
            throw new IllegalArgumentException(s"A packet channel with id '$id' is already registered to this channel list !")

        openedPacketChannels.put(id, packetChannel)
        notifier.onPacketChannelRegistered(packetChannel)
    }


    def injectPacket(packet: Packet, channelID: Int): Unit = {
        openedPacketChannels(channelID)
                .addPacket(packet)
    }

    def unregisterManager(id: Int, reason: Reason): Unit = {
        val opt = openedPacketChannels.remove(id)
        if (opt.isDefined)
            notifier.onPacketChannelUnregistered(opt.get, reason)
    }

    def sendPacket(packet: Packet, coords: PacketCoordinates): Unit = {
        socket.write(packetManager.toBytes(packet, coords))
        notifier.onPacketSent(packet)
    }

    def notifyPacketUsed(packet: Packet): Unit = notifier.onPacketUsed(packet)

    override def close(reason: Reason): Unit = {
        for ((_, channel) <- openedPacketChannels) {
            channel.close(reason)
        }
    }
}