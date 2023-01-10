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

package fr.linkit.server.config

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.persistence.ObjectTranslator
import fr.linkit.api.internal.system.security.BytesHasher
import fr.linkit.engine.gnom.persistence.DefaultObjectTranslator
import ServerConnectionConfigBuilder.count

import java.net.URL

abstract class ServerConnectionConfigBuilder {

    var maxConnection                 : Int               = Int.MaxValue
    var nWorkerThreadFunction         : Int => Int        = _ + 2 //1 thread per client connection + 2 for the server connection
    var configName                    : String            = s"config#$count"
    var hasher                        : BytesHasher       = BytesHasher.inactive
    var identifierAmbiguityStrategy   : AmbiguityStrategy = AmbiguityStrategy.REJECT_NEW
    var defaultPersistenceConfigScript: Option[URL]       = None
    val identifier: String
    val port      : Int

    //Can't be configurable for now
    final val translatorFactory: ApplicationContext => ObjectTranslator = new DefaultObjectTranslator(_)

    def build(): ServerConnectionConfiguration = {
        val builder = this
        count += 1
        new ServerConnectionConfiguration {
            override val connectionName: String = builder.identifier
            override val port          : Int    = builder.port
            override val defaultPersistenceConfigScript: Option[URL]                            = builder.defaultPersistenceConfigScript
            override val maxConnection                 : Int                                    = builder.maxConnection
            override val nWorkerThreadFunction         : Int => Int                             = builder.nWorkerThreadFunction
            override val configName                    : String                                 = builder.configName
            override val hasher                        : BytesHasher                            = builder.hasher
            override val translatorFactory             : ApplicationContext => ObjectTranslator = builder.translatorFactory
            override val identifierAmbiguityStrategy   : AmbiguityStrategy                      = builder.identifierAmbiguityStrategy
        }
    }

}

object ServerConnectionConfigBuilder {

    private var count = 1

    implicit def autoBuild(configBuilder: ServerConnectionConfigBuilder): ServerConnectionConfiguration = {
        configBuilder.build()
    }
}
