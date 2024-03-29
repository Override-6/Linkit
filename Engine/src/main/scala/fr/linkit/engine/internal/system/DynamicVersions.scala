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

import fr.linkit.api.internal.system.{Version, Versions}

class DynamicVersions() extends Versions {

    private var api            = Version.Unknown
    private var Engine         = Version.Unknown
    private var implementation = Version.Unknown

    def setApiVersion(version: Version): this.type = {
        api = version
        this
    }

    def setEngineVersion(version: Version): this.type = {
        Engine = version
        this
    }

    def setImplementationVersion(version: Version): this.type = {
        implementation = version
        this
    }

    def setAll(other: Versions): Unit = {
        if (other == null)
            throw new NullPointerException

        api = other.apiVersion
        Engine = other.engineVersion
        implementation = other.implementationVersion
    }

    override def apiVersion: Version = api

    override def engineVersion: Version = Engine

    override def implementationVersion: Version = implementation

}

object DynamicVersions {

    def unknown: DynamicVersions = new DynamicVersions

    def from(other: Versions): DynamicVersions = {
        val versions = new DynamicVersions()
        versions.setAll(other)
        versions
    }
}
