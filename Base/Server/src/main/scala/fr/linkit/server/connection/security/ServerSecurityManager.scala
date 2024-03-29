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

package fr.linkit.server.connection.security

import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.application.config.ConnectionConfiguration
import fr.linkit.api.internal.system.security.{ApplicationSecurityManager, BytesHasher}

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
