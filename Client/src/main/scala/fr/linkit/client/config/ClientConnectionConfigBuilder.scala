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

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.persistence.ObjectTranslator
import fr.linkit.api.internal.system.security.BytesHasher
import fr.linkit.engine.gnom.persistence.DefaultObjectTranslator

import java.net.{InetSocketAddress, Socket, URL}

abstract class ClientConnectionConfigBuilder {

    var reconnectionMillis            : Int                                    = 5000
    var socketFactory                 : InetSocketAddress => Socket            = s => new Socket(s.getAddress, s.getPort)
    var configName                    : String                                 = "simple-config"
    var hasher                        : BytesHasher                            = BytesHasher.inactive
    var translatorFactory             : ApplicationContext => ObjectTranslator = new DefaultObjectTranslator(_)
    var defaultPersistenceConfigScript: Option[URL]                            = None
    val identifier   : String
    val remoteAddress: InetSocketAddress

    /**
     * @return a [[ClientConnectionConfiguration]] based on the current settings
     * */
    def buildConfig(): ClientConnectionConfiguration = {
        val builder = this
        new ClientConnectionConfiguration {
            override val reconnectionMillis            : Int                                    = builder.reconnectionMillis
            override val socketFactory                 : InetSocketAddress => Socket            = builder.socketFactory
            override val remoteAddress                 : InetSocketAddress                      = builder.remoteAddress
            override val configName                    : String                                 = builder.configName
            override val identifier                    : String                                 = builder.identifier
            override val hasher                        : BytesHasher                            = builder.hasher
            override val translatorFactory             : ApplicationContext => ObjectTranslator = builder.translatorFactory
            override val defaultPersistenceConfigScript: Option[URL]                            = builder.defaultPersistenceConfigScript
        }: ClientConnectionConfiguration
    }

}

object ClientConnectionConfigBuilder {

    implicit def autoBuild(builder: ClientConnectionConfigBuilder): ClientConnectionConfiguration = {
        builder.buildConfig()
    }

}