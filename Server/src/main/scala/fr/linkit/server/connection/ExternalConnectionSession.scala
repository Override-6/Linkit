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

import fr.linkit.api.gnom.network.{Engine, ExternalConnectionState}
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.packet.traffic.PacketTraffic.SystemChannelID
import fr.linkit.api.gnom.persistence.PacketUpload
import fr.linkit.api.internal.concurrency.workerExecution
import fr.linkit.api.internal.system.{JustifiedCloseable, Reason}
import fr.linkit.engine.gnom.packet.traffic.ChannelScopes
import fr.linkit.engine.internal.concurrency.PacketReaderThread
import fr.linkit.engine.internal.system.SystemPacketChannel
import fr.linkit.server.connection.traffic.ConnectionOrdinalsHandler
import fr.linkit.server.network.ServerSideNetwork

import java.net.Socket

case class ExternalConnectionSession private(boundIdentifier: String,
                                             private val socket: SocketContainer,
                                             info: ExternalConnectionSessionInfo) extends JustifiedCloseable {
    
    val server           : ServerConnection           = info.server
    val network          : ServerSideNetwork          = info.network
    val connectionManager: ExternalConnectionsManager = info.manager
    val readThread       : PacketReaderThread         = info.readThread
    val serverTraffic    : PacketTraffic              = server.traffic
    val ordinals         : ConnectionOrdinalsHandler  = new ConnectionOrdinalsHandler()
    val channel          : SystemPacketChannel        = serverTraffic.getInjectable(SystemChannelID, SystemPacketChannel, ChannelScopes.include(boundIdentifier))
    
    @workerExecution
    override def close(reason: Reason): Unit = {
        socket.close(reason)
        //tasksHandler.close()
        network.removeEngine(boundIdentifier)
        serverTraffic.close(reason)
    }
    
    def getSocketState: ExternalConnectionState = socket.getState
    
    def send(result: PacketUpload): Unit = {
        socket.write(result.buff(() => ordinals.forChannel(result.coords.path).next()))
    }
    
    def updateSocket(socket: Socket): Unit = this.socket.set(socket)
    
    def getEntity: Engine = network.findEngine(boundIdentifier).get
    
    override def isClosed: Boolean = socket.isClosed //refers to an used closeable element
}
