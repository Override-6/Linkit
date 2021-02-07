package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.packet.Packet
import org.jetbrains.annotations.NotNull

class SocketPacketTraffic(@NotNull relay: Relay,
                          @NotNull socket: DynamicSocket) extends AbstractPacketTraffic(relay.configuration, relay.identifier) {

    private val translator = relay.packetTranslator

    override def newWriter(identifier: Int, transform: Packet => Packet): PacketWriter = {
        new SocketPacketWriter(socket, translator, WriterInfo(this, identifier, transform))
    }

}
