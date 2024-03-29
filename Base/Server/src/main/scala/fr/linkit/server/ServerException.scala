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

package fr.linkit.server

import fr.linkit.api.application.connection.ConnectionException
import fr.linkit.server.connection.ServerConnection
import org.jetbrains.annotations.NotNull

import scala.collection.mutable

class ServerException(@NotNull connection: ServerConnection,
                      msg: String, cause: Throwable = null) extends ConnectionException(connection, msg, cause) {

    override def appendMessage(sb: mutable.StringBuilder): Unit = {
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