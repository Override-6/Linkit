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

package fr.linkit.api.application.connection

import fr.linkit.api.internal.system.AppException
import org.jetbrains.annotations.Nullable

import scala.collection.mutable

class ConnectionException(@Nullable connection: ConnectionContext,
                          msg: String, cause: Throwable = null) extends AppException(msg, cause) {

    override def appendMessage(sb: mutable.StringBuilder): Unit = {
        super.appendMessage(sb)
        if (connection == null)
            return

        val identifier = connection.currentName
        sb.append("An exception occurred with connection '")
                .append(identifier)
                .append('\'')
        connection match {
            case external: ExternalConnection =>
                sb.append("\n\t")
                        .append("bound to '")
                        .append(external.boundNT)
                        .append("':\n")
        }
    }

}

object ConnectionException {

    def apply(@Nullable connection: ConnectionContext, @Nullable msg: String, @Nullable cause: Throwable = null): ConnectionException = {
        new ConnectionException(connection, msg, cause)
    }

}
