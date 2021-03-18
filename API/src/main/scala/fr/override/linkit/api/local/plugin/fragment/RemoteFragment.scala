package fr.`override`.linkit.api.local.plugin.fragment

import fr.`override`.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.`override`.linkit.api.connection.packet.traffic.PacketSender
import org.jetbrains.annotations.NotNull

trait RemoteFragment extends PluginFragment {
    val nameIdentifier: String

    def handleRequest(packet: Packet, coords: DedicatedPacketCoordinates): Unit

    @NotNull
    protected def packetSender(): PacketSender
}
