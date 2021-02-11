package fr.`override`.linkit.server.connection

import java.net.SocketException

import fr.`override`.linkit.api.packet._
import fr.`override`.linkit.api.packet.serialization.PacketTranslator.{SenderSeparator, TargetSeparator}
import fr.`override`.linkit.api.packet.traffic.{DynamicSocket, PacketInjections, PacketReader}
import fr.`override`.linkit.server.RelayServer
import org.jetbrains.annotations.Nullable

class ConnectionPacketReader(socket: DynamicSocket, server: RelayServer, @Nullable identifier: String) {

    private val packetReader = new PacketReader(socket, server.securityManager)
    private val manager = server.connectionsManager
    private val packetTranslator = server.packetTranslator
    @volatile private var concernedPacketsReceived = 0

    def nextPacket(onPacketReceived: (Packet, PacketCoordinates, Int) => Unit): Unit = {
        try {
            nextConcernedPacket(onPacketReceived(_,_, concernedPacketsReceived))
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
        if (bytes == null) {
            return
        }

        //NETWORK-DEBUG-MARK
        //println(s"received : ${new String(bytes)}")
        val target = getTargetID(bytes)

        target match {
            case server.identifier =>
                concernedPacketsReceived += 1
                val (packet, coordinates) = packetTranslator.toPacketAndCoords(bytes)
                event(packet, coordinates)

            case "BROADCAST" => server.runLater {
                manager.broadcastBytes(bytes, Array(identifier))
                val (packet, coordinates) = packetTranslator.toPacketAndCoords(bytes)
                concernedPacketsReceived += 1

                //would inject the packet into registered injectables (if some are registered)
                server.traffic.handleInjection(PacketInjections.createInjection(packet, coordinates, concernedPacketsReceived))
            }
            case _ => manager.deflectTo(bytes, target)
        }
    }

    private def getTargetID(bytes: Array[Byte]): String =
        PacketUtils.stringBetween(SenderSeparator, TargetSeparator)(bytes)

}
