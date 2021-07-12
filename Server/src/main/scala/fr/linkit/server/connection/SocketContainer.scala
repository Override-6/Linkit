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

import fr.linkit.engine.connection.packet.traffic.DynamicSocket

import java.io._
import java.net.Socket

class SocketContainer(autoReconnect: Boolean) extends DynamicSocket(autoReconnect) {

    var identifier: String = "$unknown_engine$"

    override def boundIdentifier: String = identifier

    def set(socket: Socket): Unit = this.synchronized {
        if (currentSocket != null && !autoReconnect)
            closeCurrentStreams()

        currentSocket = socket
        currentOutputStream = new BufferedOutputStream(currentSocket.getOutputStream)
        currentInputStream = new BufferedInputStream(currentSocket.getInputStream)
        notifyAll()
        markAsConnected()
    }

    def getCurrent: Socket = currentSocket

    override protected def handleReconnection(): Unit = {
        this.synchronized {
            try {
                wait()
            } catch {
                case _: InterruptedException => //thrown when the reconnection is brutally stopped (ex: server stopped, critical error...)
            }
        }
    }

}