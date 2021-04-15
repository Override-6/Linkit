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

package fr.linkit.api.local.resource

import fr.linkit.api.local.system.fsa.FileAdapter

/**
 * An external resource is considered as external as long as it is not stored into an archived file.
 * The resource can be partially present on a driver of the current machine, or can be partially stored
 * into a distant driver.
 * */
trait ExternalResource {
    /**
     * Each times a resource is modified from a LinkitApplication,
     * its ResourceVersion must be updated by the modifier application.
     * This can be useful to determine if a resource is obsolete depending on his
     * utilisation (example: a file used by the core which have not the same [[ResourceVersions.coreVersion]] could be out of date)
     * */
    def getLastModified: ResourceVersions

    /**
     * @return The file adapter that represent this resource on the os's File System
     * */
    def getAdapter: FileAdapter

    /**
     * @return The checksum of this resource.
     * */
    def getChecksum: Long

}
