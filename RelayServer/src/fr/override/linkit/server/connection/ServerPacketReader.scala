package fr.`override`.linkit.server.connection

import java.net.SocketException

import fr.`override`.linkit.server.RelayServer
import fr.`override`.linkit.api.packet._
import org.jetbrains.annotations.Nullable

class ServerPacketReader(socket: DynamicSocket, server: RelayServer, @Nullable identifier: String) {

    private val packetReader = new PacketReader(socket, server.securityManager)
    private val manager = server.connectionsManager
    private val packetManager = server.packetManager

    //TODO exceptions catches
    def nextPacket(onPacketReceived: (Packet, PacketCoordinates) => Unit): Unit = {
        try {
            nextConcernedPacket(onPacketReceived)
        } catch {
            case e: SocketException if e.getMessage == "Connection reset" =>
                val msg =
                    if (identifier == null) "socket connection reset while initialising connection."
                    else s"client '$identifier' disconnected."
                Console.err.println(msg)
        }
    }

    private def nextConcernedPacket(event: (Packet, PacketCoordinates) => Unit): Unit = {
        val bytes = packetReader.readNextPacketBytes()
        if (bytes == null)
            return
        val target = getTargetID(bytes)

        if (target == RelayServer.Identifier) { //check if packet concerns server
            val (packet, coordinates) = packetManager.toPacket(bytes)
            event(packet, coordinates)
            return
        }

        manager.deflectTo(bytes, target)
    }

    private def getTargetID(bytes: Array[Byte]): String =
        PacketUtils.cutString(PacketManager.SenderSeparator, PacketManager.TargetSeparator)(bytes)

}
