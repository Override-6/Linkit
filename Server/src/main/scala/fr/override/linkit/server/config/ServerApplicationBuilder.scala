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
import fr.`override`.linkit.core.local.system.fsa.JDKFileSystemAdapters
import fr.`override`.linkit.server.ServerApplicationContext
import org.jetbrains.annotations.{NotNull, Nullable}

import scala.util.control.NonFatal

abstract class ServerApplicationBuilder {

    @Nullable var pluginFolder: String                                  = "/Plugins"
    @NotNull var fsAdapter    : FileSystemAdapter                       = JDKFileSystemAdapters.Nio
    @NotNull var loadSchematic: AppSchematic[ServerApplicationContext]  = EmptySchematic[ServerApplicationContext]

    @throws[ApplicationInstantiationException]("If any exception is thrown during build")
    def build(): ServerApplicationContext = {
        val builder = this
        val config = new ServerApplicationConfiguration {
            override val pluginFolder: String = builder.pluginFolder
            override val fsAdapter: FileSystemAdapter = builder.fsAdapter
        }

        try {
            val serverAppContext = new ServerApplicationContext(config)
            loadSchematic.setup(serverAppContext)
            serverAppContext
        } catch {
            case NonFatal(e) =>
                throw new ApplicationInstantiationException("Could not instantiate Server Application.", e)
        }
    }

}

//TODO Java style Builder
object ServerApplicationBuilder {

    implicit def autoBuild(builder: ServerApplicationBuilder): ServerApplicationContext = {
        builder.build()
    }

}
