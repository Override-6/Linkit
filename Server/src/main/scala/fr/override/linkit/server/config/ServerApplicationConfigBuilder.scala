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

package fr.`override`.linkit.server.config

import fr.`override`.linkit.api.local.system.config.ApplicationInstantiationException
import fr.`override`.linkit.api.local.system.config.schematic.{AppSchematic, EmptySchematic}
import fr.`override`.linkit.api.local.system.fsa.FileSystemAdapter
import fr.`override`.linkit.api.local.system.security.ApplicationSecurityManager
import fr.`override`.linkit.core.local.system.fsa.JDKFileSystemAdapters
import fr.`override`.linkit.server.ServerApplication
import org.jetbrains.annotations.{NotNull, Nullable}

abstract class ServerApplicationConfigBuilder {

    var mainPoolThreadCount: Int = 2
    @Nullable var pluginsFolder: Option[String] = Some("/Plugins")
    @NotNull var fsAdapter: FileSystemAdapter = JDKFileSystemAdapters.Nio
    @NotNull var securityManager: ApplicationSecurityManager = ApplicationSecurityManager.none
    @NotNull var loadSchematic: AppSchematic[ServerApplication] = EmptySchematic[ServerApplication]

    @throws[ApplicationInstantiationException]("If any exception is thrown during build")
    def buildConfig(): ServerApplicationConfiguration = {
        val builder = this
        new ServerApplicationConfiguration {
            override val pluginFolder: Option[String] = builder.pluginsFolder
            override val fsAdapter: FileSystemAdapter = builder.fsAdapter
            override val securityManager: ApplicationSecurityManager = builder.securityManager
            override val mainPoolThreadCount: Int = builder.mainPoolThreadCount
            override var loadSchematic: AppSchematic[ServerApplication] = builder.loadSchematic
        }
    }

}

//TODO Java style Builder
object ServerApplicationConfigBuilder {

    implicit def autoBuild(builder: ServerApplicationConfigBuilder): ServerApplicationConfiguration = {
        builder.buildConfig()
    }

}
