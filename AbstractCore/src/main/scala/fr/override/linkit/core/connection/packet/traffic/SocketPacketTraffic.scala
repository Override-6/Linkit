package fr.`override`.linkit.core.connection.packet.traffic

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.connection.packet.Packet
import fr.`override`.linkit.api.connection.packet.traffic.PacketWriter
import org.jetbrains.annotations.NotNull

class SocketPacketTraffic(@NotNull connection: ConnectionContext,
                          @NotNull socket: DynamicSocket,
                          @NotNull override val ownerID: String) extends AbstractPacketTraffic(relay.identifier) {

    private val translator = connection.packetTranslator

    override def newWriter(identifier: Int, transform: Packet => Packet): PacketWriter = {
        new SocketPacketWriter(socket, translator, WriterInfo(this, identifier, transform))
    }

}
