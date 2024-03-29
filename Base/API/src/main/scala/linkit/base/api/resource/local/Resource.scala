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

package linkit.base.api.resource.local

import fr.linkit.api.internal.system.Versions
import org.jetbrains.annotations.{NotNull, Nullable}

import java.nio.file.Path

/**
 * An external resource is considered as external as long as it is not stored into an archived file.
 * The resource can be partially present on a driver of the current machine, or can be partially stored
 * into a distant driver.
 * */
trait Resource extends AutoCloseable {

    val name: String

    def getEntry: ResourceEntry[Resource]

    def getLocation: String

    /**
     * Each times a resource is modified from a LinkitApplication,
     * its ResourceVersion must be updated by the modifier application.
     * This can be useful to determine if a resource is obsolete depending on his
     * utilisation (example: a file used by the core which have not the same [[Versions.engineVersion]] could be out of date)
     * */
    def getLastModified: Versions

    /**
     * @return the parent folder of this resource.
     * */
    def getParent: Option[ResourceFolder]

    @NotNull
    def getRoot: ResourceFolder

    /**
     * @return The file adapter that represent this resource on the os's File System
     * */
    def getPath: Path

    /**
     * @return The current checksum of this resource.
     * */
    def getChecksum: Long

    /**
     * @return The last known checksum of this resource.
     * */
    def getLastChecksum: Long


}