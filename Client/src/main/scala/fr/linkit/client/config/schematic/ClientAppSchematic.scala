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

package fr.linkit.client.config.schematic

import fr.linkit.api.application.config.ApplicationInstantiationException
import fr.linkit.api.application.config.schematic.AppSchematic
import fr.linkit.client.ClientApplication
import fr.linkit.client.config.ClientConnectionConfiguration

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

abstract class ClientAppSchematic extends AppSchematic[ClientApplication] {

    protected[schematic] val serverConfigs: ListBuffer[ClientConnectionConfiguration] =
        ListBuffer.empty[ClientConnectionConfiguration]

    @throws[ApplicationInstantiationException]
    override def setup(app: ClientApplication): Unit = {
        for (configuration <- serverConfigs) {
            app.runLaterControl {
                try {
                    app.openConnection(configuration)
                } catch {
                    case NonFatal(e) =>
                        val name: String = configuration.configName
                        throw new ApplicationInstantiationException(s"Failed to load configuration '$name'", e)
                }
            }.throwNextThrowable()
        }
    }

}