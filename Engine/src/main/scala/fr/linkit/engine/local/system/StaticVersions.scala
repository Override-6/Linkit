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

package fr.linkit.engine.local.system

import fr.linkit.api.local.system.{ApiConstants, Version, Versions}

case class StaticVersions(override val apiVersion: Version,
                          override val EngineVersion: Version,
                          override val implementationVersion: Version) extends Versions {
}

object StaticVersions {

    private var currentVersions: StaticVersions = _

    def currentVersion: Versions = {

        if (currentVersions != null)
            return currentVersions

        val implVersionString = System.getProperty(EngineConstants.ImplVersionProperty)
        if (implVersionString == null)
            throw new IllegalStateException(s"System property ${EngineConstants.ImplVersionProperty} was not found !")

        val implVersion = Version(implVersionString)
        currentVersions = StaticVersions(ApiConstants.Version, EngineConstants.Version, implVersion)
        currentVersions
    }
}
