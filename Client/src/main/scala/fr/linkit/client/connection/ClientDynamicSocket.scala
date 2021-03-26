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

package fr.linkit.client.connection

import fr.linkit.client.connection.ClientDynamicSocket.UnsetIdentifier
import fr.linkit.core.connection.packet.traffic.DynamicSocket
import fr.linkit.core.local.system.ContextLogger

import java.io._
import java.net.{ConnectException, InetSocketAddress, Socket, SocketException}

class ClientDynamicSocket(boundAddress: InetSocketAddress,
                          socketFactory: InetSocketAddress => Socket) extends DynamicSocket(true) {

    var identifier: String = UnsetIdentifier
    override def boundIdentifier: String = identifier

    var reconnectionPeriod: Int = 5000

    private def newSocket(): Unit = {
        closeCurrentStreams()
        currentSocket = socketFactory(boundAddress)
        currentOutputStream = new BufferedOutputStream(currentSocket.getOutputStream)
        currentInputStream = new BufferedInputStream(currentSocket.getInputStream)
    }

    override protected def handleReconnection(): Unit = {
        try {
            newSocket()
        } catch {
            case _@(_: SocketException | _: ConnectException) =>
                ContextLogger.warn("Unable to connect to server.")
                ContextLogger.warn(s"Waiting for $reconnectionPeriod ms before another try...")
                Thread.sleep(reconnectionPeriod)
                handleReconnection()
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
    private val UnsetIdentifier = "$Unset£"
}
