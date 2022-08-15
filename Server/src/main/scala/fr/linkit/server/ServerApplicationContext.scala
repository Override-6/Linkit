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

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.application.connection.NoSuchConnectionException
import fr.linkit.server.config.ServerConnectionConfiguration
import fr.linkit.server.connection.ServerConnection

trait ServerApplicationContext extends ApplicationContext {

    @throws[NoSuchConnectionException]("If the connection isn't found in the application's cache.")
    def unregister(serverConnection: ServerConnection): Unit

    def openServerConnection(configuration: ServerConnectionConfiguration): ServerConnection

    override def listConnections: Iterable[ServerConnection]

    override def findConnection(identifier: String): Option[ServerConnection]

    override def findConnection(port: Int): Option[ServerConnection]
}
