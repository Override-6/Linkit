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

package fr.linkit.api.application

import fr.linkit.api.application.config.ApplicationConfiguration
import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.application.resource.external.ResourceFolder
import fr.linkit.api.gnom.referencing.StaticNetworkObject
import fr.linkit.api.internal.concurrency.{ProcrastinatorControl, workerExecution}
import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.api.internal.system.Versions

trait ApplicationContext extends StaticNetworkObject[ApplicationReference.type] with ProcrastinatorControl {
    
    val versions: Versions

    val compilerCenter: CompilerCenter

    def countConnections: Int

    def getAppResources: ResourceFolder

    @workerExecution
    def shutdown(): Unit

    def isAlive: Boolean

    def listConnections: Iterable[ConnectionContext]

    def findConnection(identifier: String): Option[ConnectionContext]

    def findConnection(port: Int): Option[ConnectionContext]

}
