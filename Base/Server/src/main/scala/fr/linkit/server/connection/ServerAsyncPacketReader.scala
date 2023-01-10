/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.server.connection

import fr.linkit.api.gnom.network.tag.{NameTag, Server}
import fr.linkit.api.gnom.packet.traffic.AsyncPacketReader
import fr.linkit.api.gnom.packet.{BroadcastPacketCoordinates, DedicatedPacketCoordinates}
import fr.linkit.api.gnom.persistence.PacketDownload
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.packet.traffic.{DefaultAsyncPacketReader, DynamicSocket, SocketClosedException}

import java.net.SocketException

class ServerAsyncPacketReader(socket : DynamicSocket,
                              server : ServerConnection,
                              manager: ExternalConnectionsManager,
                              boundNT: NameTag) extends AsyncPacketReader {

    private val selector     = server.network
    private val simpleReader = new DefaultAsyncPacketReader(socket, server, server.traffic, server.translator)

    override def nextPacket(callback: PacketDownload => Unit): Unit = {
        try {
            nextConcernedPacket(callback)
        } catch {
            case e: SocketException if e.getMessage == "Connection reset" =>
                AppLoggers.Connection.error(s"client '$boundNT' disconnected.")
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
                if (selector.isEquivalent(dedicated.targetNT, Server)) {
                    callback(result)
                } else {
                    manager.deflect(result)
                }

            case broadcast: BroadcastPacketCoordinates =>
                throw new UnsupportedOperationException("Broadcast not supported")
            /*val identifiers = broadcast.listDiscarded(manager.listIdentifiers) ++ Array(boundIdentifier)
            manager.broadcastPacket(new DeflectedPacket(result.buff, broadcast), identifiers: _*)

            //should inject into the server too if targeted
            if (broadcast.targetIDs.contains(serverIdentifier) != broadcast.discardTargets) {
                callback(result)
            }*/
        }
    }

}
