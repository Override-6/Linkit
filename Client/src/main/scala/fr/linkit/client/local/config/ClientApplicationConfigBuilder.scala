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

package fr.linkit.client.local.config

import fr.linkit.api.local.system.config.schematic.{AppSchematic, EmptySchematic}
import fr.linkit.api.local.system.fsa.FileSystemAdapter
import fr.linkit.api.local.system.security.ApplicationSecurityManager
import fr.linkit.client.ClientApplication
import fr.linkit.engine.local.system.fsa.LocalFileSystemAdapters

abstract class ClientApplicationConfigBuilder {

    private final val enableEventHandling: Boolean = false //still in development

    val resourcesFolder: String
    var loadSchematic        : AppSchematic[ClientApplication] = new EmptySchematic()
    var nWorkerThreadFunction: Int => Int                      = _ * 2 + 2 //2 threads per external connection + 2 threads for application.
    var pluginFolder         : Option[String]                  = Some("/Plugins")
    var fsAdapter            : FileSystemAdapter               = LocalFileSystemAdapters.Nio
    var securityManager      : ApplicationSecurityManager      = ApplicationSecurityManager.none

    def buildConfig(): ClientApplicationConfiguration = {
        val builder = this
        new ClientApplicationConfiguration {
            override val loadSchematic        : AppSchematic[ClientApplication] = builder.loadSchematic
            override val enableEventHandling  : Boolean                         = builder.enableEventHandling
            override val nWorkerThreadFunction: Int => Int                      = builder.nWorkerThreadFunction
            override val pluginFolder         : Option[String]                  = builder.pluginFolder
            override val resourceFolder       : String                          = builder.resourcesFolder
            override val fsAdapter            : FileSystemAdapter               = builder.fsAdapter
            override val securityManager      : ApplicationSecurityManager      = builder.securityManager
        }
    }
}

object ClientApplicationConfigBuilder {

    implicit def autoBuild(builder: ClientApplicationConfigBuilder): ClientApplicationConfiguration = {
        builder.buildConfig()
    }
}


