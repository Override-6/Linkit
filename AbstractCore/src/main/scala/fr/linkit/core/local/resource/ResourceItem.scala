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

package fr.linkit.core.local.resource

import fr.linkit.api.local.resource.representation.ResourceRepresentation
import fr.linkit.core.local.system.DynamicVersions

case class ResourceItem(name: String) extends Serializable {

    def this() = {
        this("")
    }

    var isLocalOnly : Boolean         = false
    var lastChecksum: Long            = -1
    var lastModified: DynamicVersions = DynamicVersions.unknown

}

object ResourceItem {

    def minimal(resource: ResourceRepresentation, isLocalOnly: Boolean): ResourceItem = {
        val item = new ResourceItem(resource.name)
        item.isLocalOnly = isLocalOnly
        item
    }

    def apply(resource: ResourceRepresentation, isLocalOnly: Boolean): ResourceItem = {
        val item = new ResourceItem(resource.name)
        item.isLocalOnly = isLocalOnly
        item.lastChecksum = resource.getLastChecksum
        item.lastModified = resource.getLastModified match {
            case null                     => DynamicVersions.unknown
            case dynamic: DynamicVersions => dynamic
            case other                    => DynamicVersions.from(other)
        }
        item
    }
}
