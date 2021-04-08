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

package fr.linkit.api.local

import fr.linkit.api.connection.ConnectionContext
import fr.linkit.api.local.concurrency.{Procrastinator, workerExecution}
import fr.linkit.api.local.plugin.PluginManager
import fr.linkit.api.local.system.Version
import fr.linkit.api.local.system.config.ApplicationConfiguration

//TODO Recap :
//TODO Rewrite/write Doc and README of API, RelayServer and RelayPoint
//TODO Design a better event hooking system (Object EventCategories with sub parts like ConnectionListeners, PacketListeners, TaskListeners...)
//TODO Replace all Any types by Serializable types into network.cache
object ApplicationContext {

    val ApiVersion: Version = Version(name = "API", code = "0.20.0", stable = false)
}

trait ApplicationContext extends Procrastinator {

    val configuration: ApplicationConfiguration

    def pluginManager: PluginManager

    def countConnections: Int

    @workerExecution
    def shutdown(): Unit

    def isAlive: Boolean

    def listConnections: Iterable[ConnectionContext]

}
