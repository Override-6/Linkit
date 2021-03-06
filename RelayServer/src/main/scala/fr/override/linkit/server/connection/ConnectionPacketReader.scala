package fr.`override`.linkit.server.connection

import fr.`override`.linkit.api.packet._
import fr.`override`.linkit.api.packet.traffic.{DynamicSocket, PacketInjections, PacketReader}
import fr.`override`.linkit.server.RelayServer
import org.jetbrains.annotations.Nullable

import java.net.SocketException
import scala.util.control.NonFatal

class ConnectionPacketReader(socket: DynamicSocket, server: RelayServer, @Nullable identifier: String) {

    private val packetReader = new PacketReader(socket, server.securityManager)
    private val manager = server.connectionsManager
    private val packetTranslator = server.packetTranslator
    @volatile private var concernedPacketsReceived = 0

    def nextPacket(onPacketReceived: (Packet, DedicatedPacketCoordinates, Int) => Unit): Unit = {
        try {
            nextConcernedPacket(onPacketReceived(_, _, _))
        } catch {
            case e: SocketException if e.getMessage == "Connection reset" =>
                val msg =
                    if (identifier == null) "socket connection reset while initialising connection."
                    else s"client '$identifier' disconnected."
                Console.err.println(msg)
        }
    }

    private def nextConcernedPacket(event: (Packet, DedicatedPacketCoordinates, Int) => Unit): Unit = try {
        val bytes = packetReader.readNextPacketBytes()
        if (bytes == null) {
            return
        }

        //NETWORK-DEBUG-MARK
        println(s"received ($identifier): ${new String(bytes.take(1000)).replace('\n', ' ').replace('\r', ' ')} (l: ${bytes.length})")
        concernedPacketsReceived += 1 //let's suppose that the received packet is sent to the server.
        val packetNumber = concernedPacketsReceived
        server.runLater {
            handleBytes(bytes, event(_, _, packetNumber))
        }
    } catch {
        case NonFatal(e) => e.printStackTrace(Console.out)
    }

    private def handleBytes(bytes: Array[Byte], event: (Packet, DedicatedPacketCoordinates) => Unit): Unit = {
        val (packet, coordinates) = packetTranslator.toPacketAndCoords(bytes)
        coordinates match {
            case dedicated: DedicatedPacketCoordinates =>
                if (dedicated.targetID == server.identifier) {
                    event(packet, dedicated)
                } else {
                    manager.deflectTo(bytes, dedicated.targetID)
                    concernedPacketsReceived -= 1 //reduce the number of concerned packets because this packet did not target the server
                }


            case broadcast: BroadcastPacketCoordinates =>
                //TODO optimise packet deflection : only serialize the new coordinates, then concat the packet bytes
                val connectionsManager = server.connectionsManager
                val identifiers = broadcast.listDiscarded(connectionsManager.listIdentifiers) ++ Array(identifier)
                manager.broadcastBytes(packet, broadcast.injectableID, identifier, identifiers: _*)

                //would inject into the server too
                val coords = DedicatedPacketCoordinates(broadcast.injectableID, server.identifier, broadcast.senderID)
                val injection = PacketInjections.createInjection(packet, coords, concernedPacketsReceived)
                server.traffic.handleInjection(injection)
        }
    }

}
