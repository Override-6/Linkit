package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.packet.traffic.DynamicSocket
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import org.jetbrains.annotations.NotNull

class DedicatedPacketTraffic(@NotNull relay: Relay,
                             @NotNull socket: DynamicSocket,
                             @NotNull ownerID: String) extends AbstractPacketTraffic(relay, ownerID) {

    private val packetTranslator = relay.packetTranslator

    override def send(packet: Packet, coordinates: PacketCoordinates): Unit = {
        if (socket.isOpen) {
            val bytes = packetTranslator.fromPacketAndCoords(packet, coordinates)
            socket.write(bytes)
        }
    }
}