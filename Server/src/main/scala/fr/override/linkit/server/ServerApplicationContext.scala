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

package fr.`override`.linkit.server

import fr.`override`.linkit.api.local.ApplicationContext
import fr.`override`.linkit.api.local.plugin.PluginManager
import fr.`override`.linkit.core.local.plugin.LinkitPluginManager
import fr.`override`.linkit.server.config.{ServerApplicationConfiguration, ServerConnectionConfiguration}
import fr.`override`.linkit.server.connection.ServerConnection

class ServerApplicationContext(override val configuration: ServerApplicationConfiguration) extends ApplicationContext {

    override val pluginManager  : PluginManager     = new LinkitPluginManager(configuration.fsAdapter)

    def openServerConnection(configuration: ServerConnectionConfiguration): ServerConnection = {
        val serverConnection = new ServerConnection(configuration)
    }

}
