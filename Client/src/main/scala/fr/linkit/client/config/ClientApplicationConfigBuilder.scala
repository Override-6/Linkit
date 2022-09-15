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

package fr.linkit.client.config

import fr.linkit.api.application.config.schematic.{AppSchematic, EmptySchematic}
import fr.linkit.api.internal.system.security.ApplicationSecurityManager
import fr.linkit.client.ClientApplication

abstract class ClientApplicationConfigBuilder {


    val resourcesFolder: String
    var loadSchematic           : AppSchematic[ClientApplication] = new EmptySchematic()
    var nWorkerThreadFunction   : Int => Int                      = _ * 2 + 2 //2 threads per external connection + 2 threads for application.
    var pluginFolder            : Option[String]                  = Some("/Plugins")
    var securityManager         : ApplicationSecurityManager      = ApplicationSecurityManager.None

    def buildConfig(): ClientApplicationConfiguration = {
        val builder = this
        new ClientApplicationConfiguration {
            override val loadSchematic        : AppSchematic[ClientApplication] = builder.loadSchematic
            override val nWorkerThreadFunction: Int => Int                      = builder.nWorkerThreadFunction
            override val resourceFolder       : String                          = builder.resourcesFolder
            override val securityManager      : ApplicationSecurityManager      = builder.securityManager
        }
    }
}

object ClientApplicationConfigBuilder {

    implicit def autoBuild(builder: ClientApplicationConfigBuilder): ClientApplicationConfiguration = {
        builder.buildConfig()
    }
}


