/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.server.connection

import fr.linkit.api.gnom.persistence.ObjectDeserializationResult
import fr.linkit.api.gnom.packet.traffic.PacketReader
import fr.linkit.api.gnom.packet.{BroadcastPacketCoordinates, DedicatedPacketCoordinates}
import fr.linkit.api.internal.concurrency.workerExecution
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.packet.traffic.{DefaultPacketReader, DynamicSocket, SocketClosedException}

import java.net.SocketException
import scala.util.control.NonFatal

class SelectivePacketReader(socket: DynamicSocket,
                            server: ServerConnection,
                            manager: ExternalConnectionsManager,
                            boundIdentifier: String) extends PacketReader {

    private val configuration    = server.configuration
    private val simpleReader     = new DefaultPacketReader(socket, server, server.traffic, server.translator)
    private val serverIdentifier = server.currentIdentifier

    override def nextPacket(@workerExecution callback: (ObjectDeserializationResult) => Unit): Unit = {
        try {
            nextConcernedPacket(callback)
        } catch {
            case e: SocketException if e.getMessage == "Connection reset" =>
                AppLogger.error(s"client '$boundIdentifier' disconnected.")
        }
    }

    private def nextConcernedPacket(callback: ObjectDeserializationResult => Unit): Unit = try {
        simpleReader.nextPacket(result => {
            handleDeserialResult(result, callback)
        })
    } catch {
        case e: SocketClosedException => Console.err.println(e)
        case NonFatal(e)              => e.printStackTrace()
    }

    private def handleDeserialResult(result: ObjectDeserializationResult, callback: ObjectDeserializationResult => Unit): Unit = {

        result.coords match {
            case dedicated: DedicatedPacketCoordinates =>
                if (dedicated.targetID == server.currentIdentifier) {
                    callback(result)
                } else {
                    manager.deflect(result)
                }

            case broadcast: BroadcastPacketCoordinates =>
                val identifiers = broadcast.listDiscarded(manager.listIdentifiers) ++ Array(boundIdentifier)
                manager.broadcastPacket(result, identifiers: _*)

                //would inject into the server too
                if (broadcast.targetIDs.contains(serverIdentifier) != broadcast.discardTargets) {
                    val coords     = DedicatedPacketCoordinates(broadcast.path, server.currentIdentifier, broadcast.senderID)
                    val attributes = result.attributes
                    server.traffic.processInjection(result.packet, attributes, coords)
                }
        }
    }

}
