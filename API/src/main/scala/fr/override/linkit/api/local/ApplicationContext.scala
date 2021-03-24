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

package fr.`override`.linkit.api.local

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.local.concurrency.{Procrastinator, workerExecution}
import fr.`override`.linkit.api.local.plugin.PluginManager
import fr.`override`.linkit.api.local.system.Version
import fr.`override`.linkit.api.local.system.config.ApplicationConfiguration

import scala.collection.mutable


//TODO Recap :
//TODO Rewrite/write Doc and README of API, RelayServer and RelayPoint
//TODO Design a better event hooking system (Object EventCategories with sub parts like ConnectionListeners, PacketListeners, TaskListeners...)
//TODO Find a solution about packets that are send into a non-registered channel : if an exception is thrown, this can cause some problems, and if not, this can cause other problems. SOLUTION : Looking for "RemoteActionDescription" that can control and get some information about an action that where made over the network.
//TODO Create a PacketTree that can let the RelaySecurityManager know the content and the structure of a packet without casting it or making weird reflection stuff.
//TODO Replace all Any types by Serializable types in network.cache
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
