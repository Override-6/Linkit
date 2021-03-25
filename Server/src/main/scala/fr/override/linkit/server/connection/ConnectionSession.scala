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

import fr.`override`.linkit.api.connection.network.{ExternalConnectionState, NetworkEntity}
import fr.`override`.linkit.api.connection.packet.serialization.{PacketSerializationResult, PacketTranslator}
import fr.`override`.linkit.api.connection.packet.traffic.PacketTraffic.SystemChannelID
import fr.`override`.linkit.api.connection.packet.traffic.{ChannelScope, PacketTraffic}
import fr.`override`.linkit.api.local.concurrency.workerExecution
import fr.`override`.linkit.api.local.system.{JustifiedCloseable, Reason}
import fr.`override`.linkit.core.connection.packet.serialization.NumberSerializer
import fr.`override`.linkit.core.local.system.SystemPacketChannel
import fr.`override`.linkit.server.config.ExternalConnectionConfiguration
import fr.`override`.linkit.server.network.ServerSideNetwork
import fr.`override`.linkit.server.task.ConnectionTasksHandler

import java.net.Socket

case class ConnectionSession private(boundIdentifier: String,
                                     private val socket: SocketContainer,
                                     info: ConnectionSessionInfo) extends JustifiedCloseable {

    val server           : ServerConnection                 = info.server
    val network          : ServerSideNetwork                    = info.network
    val connectionManager: ExternalConnectionsManager       = info.manager
    val translator       : PacketTranslator                 = server.translator
    val serverTraffic    : PacketTraffic                    = server.traffic
    val channel          : SystemPacketChannel              = serverTraffic.getInjectable(SystemChannelID, ChannelScope.reserved(boundIdentifier), SystemPacketChannel)
    val packetReader     : ConnectionPacketReader           = new ConnectionPacketReader(socket, server, connectionManager, boundIdentifier)
    val tasksHandler     : ConnectionTasksHandler           = new ConnectionTasksHandler(this)

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

    def getEntity: NetworkEntity = server.network.getEntity(boundIdentifier).get

    override def isClosed: Boolean = socket.isClosed //refers to an used closeable element
}
