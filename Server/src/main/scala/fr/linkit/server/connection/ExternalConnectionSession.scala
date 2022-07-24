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
import fr.linkit.server.connection.traffic.ordinal.{ConnectionsOrdinalsRectifier, ServerOrdinals}
import fr.linkit.server.network.ServerSideNetwork

import java.net.Socket

case class ExternalConnectionSession private(boundIdentifier: String,
                                             private val socket: SocketContainer,
                                             info: ExternalConnectionSessionInfo) extends JustifiedCloseable {
    
    private[connection] val server           : ServerConnection             = info.server
    private[connection] val network          : ServerSideNetwork            = info.network
    private[connection] val connectionManager: ExternalConnectionsManager   = info.manager
    private[connection] val readThread       : PacketReaderThread           = info.readThread
    private[connection] val traffic          : PacketTraffic                = server.traffic
    private[connection] val serverOrdinals   : ServerOrdinals               = new ServerOrdinals()
    private[server]     val ordinalsRectifier: ConnectionsOrdinalsRectifier = new ConnectionsOrdinalsRectifier(traffic)
    
    @workerExecution
    override def close(reason: Reason): Unit = {
        socket.close(reason)
        network.removeEngine(boundIdentifier)
    }
    
    def getSocketState: ExternalConnectionState = socket.getState
    
    private[connection] def send(result: PacketUpload): Unit = {
        socket.write(result.buff(() => serverOrdinals.forChannel(result.coords.path).increment()))
    }
    
    private[connection] def updateSocket(socket: Socket): Unit = this.socket.set(socket)
    
    def getEntity: Engine = network.findEngine(boundIdentifier).get
    
    override def isClosed: Boolean = socket.isClosed //refers to an used closeable element
}
