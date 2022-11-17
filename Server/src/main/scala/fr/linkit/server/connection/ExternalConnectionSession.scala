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

import fr.linkit.api.gnom.network.tag.NameTag
import fr.linkit.api.gnom.network.{Engine, ExternalConnectionState}
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.persistence.PacketUpload
import fr.linkit.api.internal.system.{JustifiedCloseable, Reason}
import fr.linkit.engine.internal.concurrency.PacketReaderThread
import fr.linkit.engine.internal.util.OrdinalCounter
import fr.linkit.server.network.ServerSideNetwork

import java.net.Socket
import scala.collection.mutable

case class ExternalConnectionSession private(boundNT           : NameTag,
                                             private val socket: SocketContainer,
                                             info              : ExternalConnectionSessionInfo) extends JustifiedCloseable {

    private[connection] val server           : ServerConnection           = info.server
    private[connection] val network          : ServerSideNetwork          = info.network
    private[connection] val connectionManager: ExternalConnectionsManager = info.manager
    private[connection] val readThread       : PacketReaderThread         = info.readThread
    private[connection] val traffic          : PacketTraffic              = server.traffic
    private[server]     val ordinalsMap                                   = mutable.HashMap.empty[Int, OrdinalCounter] //key is injectables path hash

    override def close(reason: Reason): Unit = {
        socket.close(reason)
        network.removeEngine(boundNT)
    }

    def getSocketState: ExternalConnectionState = socket.getState

    private[connection] def send(result: PacketUpload): Unit = {
        val coords   = result.coords
        val ordinals = this.ordinalsMap.getOrElseUpdate(java.util.Arrays.hashCode(coords.path), new OrdinalCounter)
        val bytes    = result.buff(() => ordinals.increment(coords.senderTag))
        socket.write(bytes)
    }

    private[connection] def updateSocket(socket: Socket): Unit = this.socket.set(socket)


    override def isClosed: Boolean = socket.isClosed //refers to an used closeable element
}
