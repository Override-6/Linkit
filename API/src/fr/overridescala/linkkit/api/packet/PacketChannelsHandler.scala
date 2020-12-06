package fr.overridescala.linkkit.api.packet

import fr.overridescala.linkkit.api.system.event.EventObserver.EventNotifier
import fr.overridescala.linkkit.api.system.{JustifiedCloseable, Reason}

import scala.collection.mutable

class PacketChannelsHandler(val notifier: EventNotifier,
                            socket: DynamicSocket,
                            packetManager: PacketManager) extends JustifiedCloseable {

    private val openedPacketChannels = mutable.Map.empty[Int, PacketChannel]

    def register(packetChannel: PacketChannel): Unit = {
        val id = packetChannel.channelID

        if (openedPacketChannels.contains(id))
            throw new IllegalArgumentException(s"A packet channel with id '$id' is already registered to this channel list !")

        openedPacketChannels.put(id, packetChannel)
        notifier.onPacketChannelRegistered(packetChannel)

    }


    def injectPacket(packet: Packet, channelID: Int): Unit = {
        openedPacketChannels(channelID)
                .injectPacket(packet)
    }

    def unregister(id: Int, reason: Reason): Unit = {
        val opt = openedPacketChannels.remove(id)
        if (opt.isDefined)
            notifier.onPacketChannelUnregistered(opt.get, reason)
    }

    def sendPacket(packet: Packet, coords: PacketCoordinates): Unit = {
        socket.write(packetManager.toBytes(packet, coords))
        notifier.onPacketSent(packet, coords)
    }

    def notifyPacketUsed(packet: Packet, coordinates: PacketCoordinates): Unit =
        notifier.onPacketUsed(packet, coordinates)

    override def close(reason: Reason): Unit = {
        for ((_, channel) <- openedPacketChannels) {
            channel.close(reason)
        }
    }
}