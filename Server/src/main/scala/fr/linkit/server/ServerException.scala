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

package fr.linkit.server

import fr.linkit.api.connection.ConnectionException
import fr.linkit.server.connection.ServerConnection
import org.jetbrains.annotations.NotNull

class ServerException(@NotNull connection: ServerConnection,
                      msg: String, cause: Throwable = null) extends ConnectionException(connection, msg, cause) {

    override def appendMessage(sb: StringBuilder): Unit = {
        super.appendMessage(sb)
        val port = connection.configuration.port
        sb.append(s"Server opened on port '$port'")
    }

}

object ServerException {

    def apply(@NotNull connection: ServerConnection, msg: String, cause: Throwable = null): ServerException = {
        if (connection == null)
            throw new NullPointerException()

        new ServerException(connection, msg, cause)
    }
}