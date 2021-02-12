package fr.`override`.linkit.server.connection

import java.net.SocketException

import fr.`override`.linkit.api.packet._
import fr.`override`.linkit.api.packet.serialization.PacketTranslator
import fr.`override`.linkit.api.packet.traffic.{DynamicSocket, PacketInjections, PacketReader}
import fr.`override`.linkit.server.RelayServer
import org.jetbrains.annotations.Nullable

import scala.util.control.NonFatal

class ConnectionPacketReader(socket: DynamicSocket, server: RelayServer, @Nullable identifier: String) {

    private val packetReader = new PacketReader(socket, server.securityManager)
    private val manager = server.connectionsManager
    private val packetTranslator = server.packetTranslator
    @volatile private var concernedPacketsReceived = 0

    def nextPacket(onPacketReceived: (Packet, PacketCoordinates, Int) => Unit): Unit = {
        try {
            nextConcernedPacket(onPacketReceived(_, _, concernedPacketsReceived))
        } catch {
            case e: SocketException if e.getMessage == "Connection reset" =>
                val msg =
                    if (identifier == null) "socket connection reset while initialising connection."
                    else s"client '$identifier' disconnected."
                Console.err.println(msg)
        }
    }

    private def nextConcernedPacket(event: (Packet, PacketCoordinates) => Unit): Unit = try {
        val bytes = packetReader.readNextPacketBytes()
        if (bytes == null) {
            return
        }

        //NETWORK-DEBUG-MARK
        println(s"received : ${new String(bytes).replace('\n', ' ')} (l: ${bytes.length})")
        server.runLater {
            handleBytes(bytes, event)
        }
    } catch {
        case NonFatal(e) => e.printStackTrace(Console.out)
    }

    private def handleBytes(bytes: Array[Byte], event: (Packet, PacketCoordinates) => Unit): Unit = {
        val (packet, coordinates) = packetTranslator.toPacketAndCoords(bytes)
        //println(s"RECEIVED PACKET $packet WITH COORDINATES $coordinates. This packet will be handled in thread ${Thread.currentThread()}")

        val target = coordinates.targetID
        if (target.startsWith(PacketTranslator.BroadcastIdentifier)) {
            //TODO optimise the packet deflection : only serialize the new coordinates, then send the packet bytes directly
            val discards = PacketTranslator.listDiscarded(target)
            manager.broadcastBytes(packet, coordinates.injectableID, identifier, discards ++ Array(identifier))
            concernedPacketsReceived += 1

            //would inject the packet into registered injectables (if some are registered)
            val injection = PacketInjections.createInjection(packet, coordinates, concernedPacketsReceived)
            server.traffic.handleInjection(injection)
            return
        }

        coordinates.targetID match {
            case server.identifier =>
                concernedPacketsReceived += 1
                event(packet, coordinates)

            case _ => manager.deflectTo(bytes, coordinates.targetID)
        }
    }

}
