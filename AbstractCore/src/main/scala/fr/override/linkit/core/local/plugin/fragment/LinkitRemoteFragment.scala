package fr.`override`.linkit.core.local.plugin.fragment

import fr.`override`.linkit.api.connection.packet.traffic.PacketSender
import fr.`override`.linkit.api.local.plugin.fragment.RemoteFragment
import fr.`override`.linkit.core.connection.packet.traffic.channel.AsyncPacketChannel
import org.jetbrains.annotations.NotNull

abstract class LinkitRemoteFragment extends LinkitPluginFragment with RemoteFragment {
    private var channel: AsyncPacketChannel = _

    val nameIdentifier: String

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
