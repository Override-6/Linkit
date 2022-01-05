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

package fr.linkit.server.config

import fr.linkit.api.application.config.ApplicationInstantiationException
import fr.linkit.api.application.config.schematic.{AppSchematic, EmptySchematic}
import fr.linkit.api.internal.system.security.ApplicationSecurityManager
import fr.linkit.server.ServerApplication
import org.jetbrains.annotations.{NotNull, Nullable}

abstract class ServerApplicationConfigBuilder {

    @NotNull val resourcesFolder: String
              var mainPoolThreadCount: Int                             = 2
    @Nullable var pluginFolder       : Option[String]                  = Some("/Plugins")
    @NotNull  var securityManager    : ApplicationSecurityManager      = ApplicationSecurityManager.none
    @NotNull  var loadSchematic      : AppSchematic[ServerApplication] = EmptySchematic[ServerApplication]

    @throws[ApplicationInstantiationException]("If any exception is thrown during build")
    def buildConfig(): ServerApplicationConfiguration = {
        val builder = this
        new ServerApplicationConfiguration {
            override val pluginFolder       : Option[String]                  = builder.pluginFolder
            override val resourceFolder     : String                          = builder.resourcesFolder
            override val securityManager    : ApplicationSecurityManager      = builder.securityManager
            override val mainPoolThreadCount: Int                             = builder.mainPoolThreadCount
            override var loadSchematic      : AppSchematic[ServerApplication] = builder.loadSchematic
        }
    }

}

//TODO Java style Builder
object ServerApplicationConfigBuilder {

    implicit def autoBuild(builder: ServerApplicationConfigBuilder): ServerApplicationConfiguration = {
        builder.buildConfig()
    }

}
