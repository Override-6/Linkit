package fr.`override`.linkit.api.`extension`.fragment

import fr.`override`.linkit.api.packet.traffic.PacketSender
import fr.`override`.linkit.api.packet.traffic.dedicated.AsyncPacketChannel
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import org.jetbrains.annotations.NotNull

abstract class RemoteFragment extends ExtensionFragment {
    private var channel: AsyncPacketChannel = _

    val nameIdentifier: String

    def handleRequest(packet: Packet, coords: PacketCoordinates): Unit

    @NotNull
    protected def packetSender(): PacketSender = {
        if (channel == null)
            throw new IllegalStateException("Attempted to send a packet from a non initialised remote fragment")
        channel
    }

    private[fragment] def setChannel(channel: AsyncPacketChannel): Unit = {
        if (this.channel != null)
            throw new IllegalStateException("Attempted to override the currently used packet channel.")

        this.channel = channel
        channel.addOnPacketReceived(handleRequest)
    }
}
