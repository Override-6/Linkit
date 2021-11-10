/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.application

import fr.linkit.api.application.config.ApplicationConfiguration
import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.application.plugin.PluginManager
import fr.linkit.api.application.resource.external.ResourceFolder
import fr.linkit.api.gnom.reference.{NetworkObject, StaticNetworkObject}
import fr.linkit.api.internal.concurrency.{ProcrastinatorControl, workerExecution}
import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.api.internal.system.Versions

//TODO Recap :
// Rewrite/write Doc and README of API
// Design a better event hooking system (Object EventCategories with sub parts like ConnectionListeners, PacketListeners, TaskListeners...)
trait ApplicationContext extends StaticNetworkObject[ApplicationReference] with ProcrastinatorControl {

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

    def findConnection(identifier: String): Option[ConnectionContext]

    def findConnection(port: Int): Option[ConnectionContext]

}
