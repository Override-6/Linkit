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

package fr.linkit.api.application.resource

import fr.linkit.api.application.resource.external.ResourceFolder
import fr.linkit.api.application.resource.exception.NoSuchResourceException
import fr.linkit.api.internal.system.Versions

/**
 * A Resource Maintainer is a helper for [[ResourceFolder]] which contains information
 * about all sub files/folders being stored into the folder that they are maintaining.
 * The way that resources information is managed is implementation-specific.
 *
 * Information could either be for files stored on the current machine, or for distant files.
 * Resources Maintainers of a folder (distant or physical) must be synchronised.
 *
 * Files and folders are not necessarily known by this maintainer. Some files could be completely
 * hidden from the extern
 * */
trait ResourcesMaintainer {

    /**
     * Return the resources folder that this maintainer handles.
     * */
    def getResources: ResourceFolder

    /**
     * @param name the name of file/folder to test.
     * @return {{{true}}} if the resource is stored distantly, {{{false}}} instead.
     * */
    def isRemoteResource(name: String): Boolean

    /**
     * @param name the name of file/folder to test.
     * @return true if this resource name is registered in the maintainer, false instead.
     * */
    def isKnown(name: String): Boolean

    /**
     * The last checksum known by the maintainer.
     * */
    @throws[NoSuchResourceException]("If the resource name is unknown.")
    def getLastChecksum(name: String): Long

    /**
     * The Versions of the last Linkit Framework which modified the resource.
     *
     * @param name the resource name to find versions of the last Linkit Framework instance which modified it.
     * */
    @throws[NoSuchResourceException]("If the resource name is unknown.")
    def getLastModified(name: String): Versions

}
