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

package fr.linkit.server.local.config.schematic

import fr.linkit.api.local.system.AppLogger
import fr.linkit.api.local.system.config.ApplicationInstantiationException
import fr.linkit.api.local.system.config.schematic.AppSchematic
import fr.linkit.server.ServerApplication
import fr.linkit.server.local.config.ServerConnectionConfiguration

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

abstract class ServerAppSchematic extends AppSchematic[ServerApplication] {

    protected[schematic] val serverConfigs: ListBuffer[ServerConnectionConfiguration] =
        ListBuffer.empty[ServerConnectionConfiguration]

    @throws[ApplicationInstantiationException]
    override def setup(a: ServerApplication): Unit = {
        for (configuration <- serverConfigs) {
            try {
                AppLogger.debug(s"Loading configuration ${configuration.configName}")
                a.openServerConnection(configuration)
            } catch {
                case NonFatal(e) =>
                    val name: String = configuration.configName
                    throw new ApplicationInstantiationException(s"Failed to load configuration '$name'", e)
            }
        }
    }
}