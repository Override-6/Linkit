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

package fr.linkit.api.local.system

/**
 * A Data class description that stores information about all the Framework's layers versions.
 * */
trait Versions {

    /**
     * The version of the Linkit Framework API
     * */
    def apiVersion: Version

    /**
     * The version of the Linkit Framework Abstract Core
     * */
    def abstractCoreVersion: Version

    /**
     * The version of the Linkit Framework Implementation.
     * */
    def implementationVersion: Version

}
