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

package fr.linkit.core.local.system

import fr.linkit.api.local.system.{Version, Versions}

class DynamicVersions() extends Versions {

    private var api            = Version.Unknown
    private var abstractCore   = Version.Unknown
    private var implementation = Version.Unknown

    def setApiVersion(version: Version): this.type = {
        api = version
        this
    }

    def setAbstractCoreVersion(version: Version): this.type = {
        abstractCore = version
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
        abstractCore = other.abstractCoreVersion
        implementation = other.implementationVersion
    }

    override def apiVersion: Version = api

    override def abstractCoreVersion: Version = abstractCore

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
