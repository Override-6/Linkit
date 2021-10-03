/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.api.application.plugin

import fr.linkit.api.internal.system.Version

import java.security.Permission

trait PluginConfiguration {

    val pluginName: String

    val pluginDescription: String

    val pluginVersion: Version

    val pluginPermissions: Array[Permission]

    def getProperty(name: String): String
}
