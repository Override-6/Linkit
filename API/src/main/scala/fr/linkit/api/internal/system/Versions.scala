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

package fr.linkit.api.internal.system

/**
 * A Data class description that stores information about all the Framework's layers versions.
 * */
trait Versions extends Serializable {

    /**
     * The version of the Linkit Framework API
     * */
    def apiVersion: Version

    /**
     * The version of the Linkit Framework Abstract Core
     * */
    def engineVersion: Version

    /**
     * The version of the Linkit Framework Implementation.
     * */
    def implementationVersion: Version

    def sameVersions(other: Versions): Boolean = {
        other.apiVersion == apiVersion &&
                other.engineVersion == engineVersion &&
                other.implementationVersion == implementationVersion
    }

}

object Versions {

    /**
     * Represents a collection of unknown versions.
     * */
    object Unknown extends Versions {

        override def apiVersion: Version = Version.Unknown

        override def engineVersion: Version = Version.Unknown

        override def implementationVersion: Version = Version.Unknown
    }

}
