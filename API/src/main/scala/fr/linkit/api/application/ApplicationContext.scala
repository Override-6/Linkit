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

package fr.linkit.api.application

import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.internal.concurrency.{ProcrastinatorControl, workerExecution}
import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.api.application.plugin.PluginManager
import fr.linkit.api.application.resource.external.ResourceFolder
import fr.linkit.api.internal.system.Versions
import fr.linkit.api.application.config.ApplicationConfiguration

//TODO Recap :
// Rewrite/write Doc and README of API
// Design a better event hooking system (Object EventCategories with sub parts like ConnectionListeners, PacketListeners, TaskListeners...)
trait ApplicationContext extends ProcrastinatorControl {

    val configuration: ApplicationConfiguration

    val versions: Versions

    val compilerCenter: CompilerCenter

    def pluginManager: PluginManager

    def countConnections: Int

    def getAppResources: ResourceFolder

    @workerExecution
    def shutdown(): Unit

    def isAlive: Boolean

    def listConnections: Iterable[ConnectionContext]

    def getConnection(identifier: String): Option[ConnectionContext]

    def getConnection(port: Int): Option[ConnectionContext]

}
