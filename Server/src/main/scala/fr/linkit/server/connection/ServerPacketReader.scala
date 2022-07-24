/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.server.connection

import fr.linkit.api.gnom.packet.traffic.PacketReader
import fr.linkit.api.gnom.packet.{BroadcastPacketCoordinates, DedicatedPacketCoordinates}
import fr.linkit.api.gnom.persistence.PacketDownload
import fr.linkit.api.internal.concurrency.workerExecution
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.packet.traffic.{DefaultPacketReader, DynamicSocket, SocketClosedException}
import fr.linkit.server.connection.traffic.DeflectedPacket

import java.net.SocketException
import scala.util.control.NonFatal

class ServerPacketReader(socket: DynamicSocket,
                         server: ServerConnection,
                         manager: ExternalConnectionsManager,
                         boundIdentifier: String) extends PacketReader {
    
    private val simpleReader     = new DefaultPacketReader(socket, server, server.traffic, server.translator)
    private val serverIdentifier = server.currentIdentifier
    
    override def nextPacket(@workerExecution callback: PacketDownload => Unit): Unit = {
        try {
            nextConcernedPacket(callback)
        } catch {
            case e: SocketException if e.getMessage == "Connection reset" =>
                AppLoggers.Connection.error(s"client '$boundIdentifier' disconnected.")
        }
    }
    
    private def nextConcernedPacket(callback: PacketDownload => Unit): Unit = try {
        simpleReader.nextPacket(result => {
            handleDeserialResult(result, callback)
        })
    } catch {
        case e: SocketClosedException => Console.err.println(e)
    }
    
    private def handleDeserialResult(result: PacketDownload, callback: PacketDownload => Unit): Unit = {
        
        result.coords match {
            case dedicated: DedicatedPacketCoordinates =>
                if (dedicated.targetID == server.currentIdentifier) {
                    callback(result)
                } else {
                    manager.deflect(result)
                }
            
            case broadcast: BroadcastPacketCoordinates =>
                val identifiers = broadcast.listDiscarded(manager.listIdentifiers) ++ Array(boundIdentifier)
                manager.broadcastPacket(new DeflectedPacket(result.buff, broadcast), identifiers: _*)
                
                //should inject into the server too if targeted
                if (broadcast.targetIDs.contains(serverIdentifier) != broadcast.discardTargets) {
                    callback(result)
                }
        }
    }
    
}
