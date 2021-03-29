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

package fr.linkit.server.connection

import java.net.SocketException
import fr.linkit.api.connection.packet.serialization.PacketDeserializationResult
import fr.linkit.api.connection.packet.traffic.PacketReader
import fr.linkit.api.connection.packet.{BroadcastPacketCoordinates, DedicatedPacketCoordinates}
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.packet.traffic.{DefaultPacketReader, DirectInjectionContainer, DynamicSocket}

import scala.util.control.NonFatal

class SelectivePacketReader(socket: DynamicSocket,
                            server: ServerConnection,
                            manager: ExternalConnectionsManager,
                            boundIdentifier: String) extends PacketReader {

    private val configuration = server.configuration
    private val simpleReader  = new DefaultPacketReader(socket, configuration.hasher, server, configuration.translator)

    override def nextPacket(@workerExecution callback: (PacketDeserializationResult) => Unit): Unit = {
        try {
            nextConcernedPacket(callback)
        } catch {
            case e: SocketException if e.getMessage == "Connection reset" =>
                AppLogger.error(s"client '$boundIdentifier' disconnected.")
        }
    }

    private def nextConcernedPacket(callback: (PacketDeserializationResult) => Unit): Unit = try {
        simpleReader.nextPacket(result => {
            handleSerialResult(result, callback)
        })
    } catch {
        case NonFatal(e) => e.printStackTrace(Console.out)
    }

    private def handleSerialResult(result: PacketDeserializationResult, callback: PacketDeserializationResult => Unit): Unit = {
        val packet = result.packet

        result.coords match {
            case dedicated: DedicatedPacketCoordinates =>
                if (dedicated.targetID == server.supportIdentifier) {
                    callback(result)
                } else {
                    manager.deflect(result)
                }

            case broadcast: BroadcastPacketCoordinates =>
                //TODO optimise packet deflection : only serialize the new coordinates, then concat the packet bytes
                val identifiers = broadcast.listDiscarded(manager.listIdentifiers) ++ Array(boundIdentifier)
                manager.broadcastBytes(packet, broadcast.injectableID, boundIdentifier, identifiers: _*)

                //would inject into the server too
                val coords = DedicatedPacketCoordinates(broadcast.injectableID, server.supportIdentifier, broadcast.senderID)
                server.traffic.handleInjection(packet, coords)
        }
    }

}
