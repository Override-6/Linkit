/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.application.resource

import fr.linkit.api.application.resource.external.Resource
import fr.linkit.engine.internal.system.DynamicVersions

case class ResourceItem(name: String) extends Serializable {

    def this() = {
        this("")
    }

    var lastChecksum: Long            = 0
    var lastModified: DynamicVersions = DynamicVersions.unknown

}

object ResourceItem {

    def minimal(resource: Resource): ResourceItem = {
        new ResourceItem(resource.name)
    }

    def apply(resource: Resource): ResourceItem = {
        val item = new ResourceItem(resource.name)
        item.lastChecksum = resource.getLastChecksum
        item.lastModified = resource.getLastModified match {
            case null                     => DynamicVersions.unknown
            case dynamic: DynamicVersions => dynamic
            case other                    => DynamicVersions.from(other)
        }
        item
    }
}
