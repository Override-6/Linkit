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

package fr.linkit.engine.internal.system

import fr.linkit.api.internal.system.{ApiConstants, Version, Versions}

case class StaticVersions(override val apiVersion: Version,
                          override val engineVersion: Version,
                          override val implementationVersion: Version) extends Versions {
}

object StaticVersions {

    private var versions: StaticVersions = _

    def currentVersions: Versions = {

        if (versions != null)
            return versions

        val implVersionString = System.getProperty(EngineConstants.ImplVersionProperty)
        if (implVersionString == null)
            throw new IllegalStateException(s"System property ${EngineConstants.ImplVersionProperty} was not found !")

        val implVersion = Version(implVersionString)
        versions = StaticVersions(ApiConstants.Version, EngineConstants.Version, implVersion)
        versions
    }
}
