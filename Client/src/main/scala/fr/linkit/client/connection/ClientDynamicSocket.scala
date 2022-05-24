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

package fr.linkit.client.connection

import fr.linkit.api.internal.system.AppLoggers
import fr.linkit.client.connection.ClientDynamicSocket.UnsetIdentifier
import fr.linkit.engine.gnom.packet.traffic.DynamicSocket

import java.io._
import java.net.{ConnectException, InetSocketAddress, Socket, SocketException}

class ClientDynamicSocket(boundAddress: InetSocketAddress,
                          socketFactory: InetSocketAddress => Socket) extends DynamicSocket(true) {

    var identifier        : String = UnsetIdentifier
    var reconnectionPeriod: Int    = 5000

    override def boundIdentifier: String = identifier

    private def newSocket(): Unit = {
        closeCurrentStreams()
        currentSocket = socketFactory(boundAddress)
        currentOutputStream = new BufferedOutputStream(currentSocket.getOutputStream)
        currentInputStream = new BufferedInputStream(currentSocket.getInputStream)
    }

    override protected def handleReconnection(): Unit = {
        var notConnected = true
        while (notConnected) {
            try {
                newSocket()
                notConnected = false
            } catch {
                case e@(_: SocketException | _: ConnectException) =>
                    AppLoggers.Traffic.warn("Unable to connect to server.")
                    AppLoggers.Traffic.warn(s"Waiting for $reconnectionPeriod ms before another try...")
                    Thread.sleep(reconnectionPeriod)
            }
        }
        markAsConnected()
    }

    def connect(identifier: String): Unit = {
        if (this.identifier != UnsetIdentifier)
            throw new IllegalStateException("The socket is already started !")

        this.identifier = identifier
        try {
            newSocket()
        } catch {
            case _@(_: SocketException | _: ConnectException) =>
                handleReconnection()
        }
        markAsConnected()
    }

}

object ClientDynamicSocket {

    private val UnsetIdentifier = "$Unset$"
}
