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

package fr.linkit.server.local.config

import fr.linkit.api.connection.packet.persistence.PacketTranslator
import fr.linkit.api.local.ApplicationContext
import fr.linkit.api.local.system.security.BytesHasher
import fr.linkit.engine.connection.packet.persistence.DefaultPacketTranslator
import fr.linkit.server.local.config.ServerConnectionConfigBuilder.count

import java.net.URL

abstract class ServerConnectionConfigBuilder {

    var maxConnection                 : Int               = Int.MaxValue
    var enableEventHandling           : Boolean           = true
    var nWorkerThreadFunction         : Int => Int        = _ * 2 + 1 //2 threads per external connection allocated + 1 for the server connection
    var configName                    : String            = s"config#$count"
    var hasher                        : BytesHasher       = BytesHasher.inactive
    var identifierAmbiguityStrategy   : AmbiguityStrategy = AmbiguityStrategy.REJECT_NEW
    var defaultPersistenceConfigScript: Option[URL]       = None
    val identifier: String
    val port      : Int

    //Can't be configurable for now
    val translatorFactory: ApplicationContext => PacketTranslator = new DefaultPacketTranslator(_)

    def build(): ServerConnectionConfiguration = {
        val builder = this
        count += 1
        new ServerConnectionConfiguration {
            override val identifier                    : String                                 = builder.identifier
            override val port                          : Int                                    = builder.port
            override val defaultPersistenceConfigScript: Option[URL]                            = builder.defaultPersistenceConfigScript
            override val maxConnection                 : Int                                    = builder.maxConnection
            override val nWorkerThreadFunction         : Int => Int                             = builder.nWorkerThreadFunction
            override val configName                    : String                                 = builder.configName
            override val hasher                        : BytesHasher                            = builder.hasher
            override val translatorFactory             : ApplicationContext => PacketTranslator = builder.translatorFactory
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
