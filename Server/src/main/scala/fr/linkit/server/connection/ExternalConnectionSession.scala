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

import fr.linkit.api.application.network.{Engine, ExternalConnectionState}
import fr.linkit.api.gnom.persistence.PacketSerializationResult
import fr.linkit.api.application.packet.traffic.PacketTraffic
import fr.linkit.api.application.packet.traffic.PacketTraffic.SystemChannelID
import fr.linkit.api.internal.concurrency.workerExecution
import fr.linkit.api.internal.system.{JustifiedCloseable, Reason}
import fr.linkit.engine.application.packet.traffic.ChannelScopes
import fr.linkit.engine.internal.concurrency.PacketReaderThread
import fr.linkit.engine.internal.system.SystemPacketChannel
import fr.linkit.server.network.ServerSideNetwork

import java.net.Socket
import java.nio.ByteBuffer

case class ExternalConnectionSession private(boundIdentifier: String,
                                             private val socket: SocketContainer,
                                             info: ExternalConnectionSessionInfo) extends JustifiedCloseable {

    val server           : ServerConnection           = info.server
    val network          : ServerSideNetwork          = info.network
    val connectionManager: ExternalConnectionsManager = info.manager
    val readThread       : PacketReaderThread         = info.readThread
    val serverTraffic    : PacketTraffic              = server.traffic
    val channel          : SystemPacketChannel        = serverTraffic.getInjectable(SystemChannelID, SystemPacketChannel, ChannelScopes.include(boundIdentifier))

    @workerExecution
    override def close(reason: Reason): Unit = {
        socket.close(reason)
        //tasksHandler.close()
        network.removeEngine(boundIdentifier)
        serverTraffic.close(reason)
    }

    def getSocketState: ExternalConnectionState = socket.getState

    def send(result: PacketSerializationResult): Unit = {
        socket.write(result.buff)
    }

    def send(buff: ByteBuffer): Unit = {
        socket.write(buff)
    }

    def updateSocket(socket: Socket): Unit = this.socket.set(socket)

    def getEntity: Engine = network.findEngine(boundIdentifier).get

    override def isClosed: Boolean = socket.isClosed //refers to an used closeable element
}
