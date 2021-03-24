/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.server.connection

import fr.`override`.linkit.api.connection.packet.{BroadcastPacketCoordinates, DedicatedPacketCoordinates, Packet}
import fr.`override`.linkit.core.connection.packet.traffic.{DynamicSocket, PacketInjections, PacketReader}
import org.jetbrains.annotations.Nullable
import java.net.SocketException

import fr.`override`.linkit.core.local.system.ContextLogger

import scala.util.control.NonFatal

class ConnectionPacketReader(socket: DynamicSocket,
                             server: ServerConnection,
                             manager: ExternalConnectionsManager,
                             @Nullable identifier: String) {

    private val packetReader = new PacketReader(socket, server.configuration.hasher)
    private val packetTranslator = server.translator
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
        val preview = new String(bytes.take(1000)).replace('\n', ' ').replace('\r', ' ')
        ContextLogger.debug(s"Received ($identifier): $preview (l: ${bytes.length})")
        concernedPacketsReceived += 1 //let's suppose that the received packet is sent to the server.
        val packetNumber = concernedPacketsReceived
        server.runLater {
            handleBytes(bytes, event(_, _, packetNumber))
        }
    } catch {
        case NonFatal(e) => e.printStackTrace(Console.out)
    }

    private def handleBytes(bytes: Array[Byte], event: (Packet, DedicatedPacketCoordinates) => Unit): Unit = {
        val (packet, coordinates) = packetTranslator.translate(bytes)
        coordinates match {
            case dedicated: DedicatedPacketCoordinates =>
                if (dedicated.targetID == server.supportIdentifier) {
                    event(packet, dedicated)
                } else {
                    manager.deflectTo(bytes, dedicated.targetID)
                    concernedPacketsReceived -= 1 //reduce the number of concerned packets because this packet did not target the server
                }


            case broadcast: BroadcastPacketCoordinates =>
                //TODO optimise packet deflection : only serialize the new coordinates, then concat the packet bytes
                val identifiers = broadcast.listDiscarded(manager.listIdentifiers) ++ Array(identifier)
                manager.broadcastBytes(packet, broadcast.injectableID, identifier, identifiers: _*)

                //would inject into the server too
                val coords = DedicatedPacketCoordinates(broadcast.injectableID, server.supportIdentifier, broadcast.senderID)
                val injection = PacketInjections.createInjection(packet, coords, concernedPacketsReceived)
                server.traffic.handleInjection(injection)
        }
    }

}
