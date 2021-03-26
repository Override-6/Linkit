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

package fr.linkit.server.security

import fr.linkit.api.connection.ConnectionContext
import fr.linkit.api.local.system.config.ConnectionConfiguration
import fr.linkit.api.local.system.security.{ApplicationSecurityManager, BytesHasher}

trait ServerSecurityManager extends ApplicationSecurityManager {
    val hasher: BytesHasher
}

object ServerSecurityManager {

    class Default extends ServerSecurityManager {
        override def checkConnection(connection: ConnectionContext): Unit = ()

        override def checkConnectionConfig(connection: ConnectionConfiguration): Unit = ()

        override val hasher: BytesHasher = BytesHasher.inactive
    }

    def default: ServerSecurityManager = new Default

}
