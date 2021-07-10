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

package fr.linkit.client.local.config.schematic

import fr.linkit.api.local.system.config.ApplicationInstantiationException
import fr.linkit.api.local.system.config.schematic.AppSchematic
import fr.linkit.client.ClientApplication
import fr.linkit.client.local.config.ClientConnectionConfiguration

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

abstract class ClientAppSchematic extends AppSchematic[ClientApplication] {

    protected[schematic] val serverConfigs: ListBuffer[ClientConnectionConfiguration] =
        ListBuffer.empty[ClientConnectionConfiguration]

    @throws[ApplicationInstantiationException]
    override def setup(app: ClientApplication): Unit = {
        for (configuration <- serverConfigs) app.runLater {
            try {
                app.openConnection(configuration)
            } catch {
                case NonFatal(e) =>
                    val name: String = configuration.configName
                    throw new ApplicationInstantiationException(s"Failed to load configuration '$name'", e)
            }
        }
    }

}