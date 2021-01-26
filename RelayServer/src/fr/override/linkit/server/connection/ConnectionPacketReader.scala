package fr.`override`.linkit.server.connection

import java.net.SocketException

import fr.`override`.linkit.api.packet._
import fr.`override`.linkit.api.packet.traffic.{DynamicSocket, PacketReader}
import fr.`override`.linkit.server.RelayServer
import org.jetbrains.annotations.Nullable

class ConnectionPacketReader(socket: DynamicSocket, server: RelayServer, @Nullable identifier: String) {

    private val packetReader = new PacketReader(socket, server.securityManager)
    private val manager = server.connectionsManager
    private val packetTranslator = server.packetTranslator

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
        //NETWORK-DEBUG-MARK
        //println(s"received : ${new String(bytes)}")
        if (bytes == null) {
            return
        }
        val target = getTargetID(bytes)

        target match {
            case server.identifier =>
                val (packet, coordinates) = packetTranslator.toPacketAndCoords(bytes)
                event(packet, coordinates)

            case "BROADCAST" =>
                manager.broadcastBytes(bytes, identifier)
                val (packet, coordinates) = packetTranslator.toPacketAndCoords(bytes)
                //handles the packet if it is registered into the server's collectors
                server.preHandlePacket(packet, coordinates)

            case _ => manager.deflectTo(bytes, target)
        }
    }

    private def getTargetID(bytes: Array[Byte]): String =
        PacketUtils.stringBetween(PacketTranslator.SenderSeparator, PacketTranslator.TargetSeparator)(bytes)

}
