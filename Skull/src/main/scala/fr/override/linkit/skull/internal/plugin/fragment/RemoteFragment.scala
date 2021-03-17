package fr.`override`.linkit.skull.internal.plugin.fragment

import fr.`override`.linkit.skull.connection.packet.traffic.PacketSender
import org.jetbrains.annotations.NotNull

trait RemoteFragment extends PluginFragment {
    val nameIdentifier: String

    @NotNull
    protected def packetSender(): PacketSender
}
