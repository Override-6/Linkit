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

import fr.linkit.api.connection.network.{ExternalConnectionState, NetworkEntity}
import fr.linkit.api.connection.packet.serialization.{PacketSerializationResult, PacketTranslator}
import fr.linkit.api.connection.packet.traffic.PacketTraffic.SystemChannelID
import fr.linkit.api.connection.packet.traffic.PacketTraffic
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.{JustifiedCloseable, Reason}
import fr.linkit.core.connection.packet.serialization.NumberSerializer
import fr.linkit.core.connection.packet.traffic.ChannelScopes
import fr.linkit.core.local.concurrency.PacketReaderThread
import fr.linkit.core.local.system.SystemPacketChannel
import fr.linkit.server.network.ServerSideNetwork
import fr.linkit.server.task.ConnectionTasksHandler

import java.net.Socket

case class ExternalConnectionSession private(boundIdentifier: String,
                                             private val socket: SocketContainer,
                                             info: ExternalConnectionSessionInfo) extends JustifiedCloseable {

    val server           : ServerConnection           = info.server
    val network          : ServerSideNetwork          = info.network
    val connectionManager: ExternalConnectionsManager = info.manager
    val readThread       : PacketReaderThread         = info.readThread
    val serverTraffic    : PacketTraffic              = server.traffic
    val channel          : SystemPacketChannel        = serverTraffic.getInjectable(SystemChannelID, ChannelScopes.retains(boundIdentifier), SystemPacketChannel)
    val tasksHandler     : ConnectionTasksHandler     = null //new ConnectionTasksHandler(this)

    @workerExecution
    override def close(reason: Reason): Unit = {
        socket.close(reason)
        tasksHandler.close()
        network.removeEntity(boundIdentifier)
        serverTraffic.close(reason)
    }

    def getSocketState: ExternalConnectionState = socket.getState

    def send(result: PacketSerializationResult): Unit = {
        socket.write(result.writableBytes)
        //val event = PacketEvents.packetWritten(result)
        //server.eventNotifier.notifyEvent(server.packetHooks, event)
    }

    def send(bytes: Array[Byte]): Unit = {
        socket.write(NumberSerializer.serializeInt(bytes.length) ++ bytes)
    }

    def updateSocket(socket: Socket): Unit = this.socket.set(socket)

    def getEntity: NetworkEntity = network.getEntity(boundIdentifier).get

    override def isClosed: Boolean = socket.isClosed //refers to an used closeable element
}
