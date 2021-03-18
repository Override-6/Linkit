package fr.`override`.linkit.core.connection.packet.traffic

import fr.`override`.linkit.api.connection.packet.Packet
import org.jetbrains.annotations.NotNull

class SocketPacketTraffic(@NotNull relay: Relay,
                          @NotNull socket: DynamicSocket,
                          @NotNull override val ownerID: String) extends AbstractPacketTraffic(relay.configuration, relay.identifier) {

    private val translator = relay.packetTranslator

    override def newWriter(identifier: Int, transform: Packet => Packet): PacketWriter = {
        new SocketPacketWriter(socket, translator, WriterInfo(this, identifier, transform))
    }

}
