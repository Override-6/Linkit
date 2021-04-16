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

import com.google.gson.Gson
import fr.linkit.api.local.resource._
import fr.linkit.api.local.system.Versions
import fr.linkit.api.local.system.fsa.FileSystemAdapter
import fr.linkit.core.local.resource.ResourceFolderMaintainer.MaintainerJSONFileName

import scala.collection.mutable.ArrayBuffer

class ResourceFolderMaintainer(maintained: ExternalResourceFolder, fsa: FileSystemAdapter) extends ResourcesMaintainer with ResourcesMaintainerInformer {

    protected val gson: Gson = new Gson()

    protected val resources: ArrayBuffer[ResourceItem] = getResourcesItems()

    override def getResources: ExternalResourceFolder = maintained

    override def isRemoteResource(name: String): Boolean = isKnown(name) && !maintained.isPresentOnDrive(name)

    override def isKnown(name: String): Boolean = name == MaintainerJSONFileName || resources.exists(_.name == name)

    override def getLastChecksum(name: String): Long = {
        resources
                .find(_.name == name)
                .map(_.lastChecksum)
                .getOrElse(throw new NoSuchResourceException(s"Resource $name is unknown for folder resource ${maintained.getLocation}"))
    }

    override def getLastModified(name: String): Versions = {
        resources
                .find(_.name == name)
                .map(_.lastModified)
                .getOrElse(throw new NoSuchResourceException(s"Resource $name is unknown for folder resource ${maintained.getLocation}"))
    }

    override def informLocalModification(name: String): Unit = ???

    def registerResource(resource: ExternalResource, localOnly: Boolean): Unit = {
        val item = new ResourceItem(resource.name) {
            override var isLocalOnly : Boolean  = localOnly
            override var lastChecksum: Long     = resource.getChecksum
            override var lastModified: Versions = resource.getLastModified
        }
        resources += item
    }

    private def store(): Unit = {

    }

    private def getResourcesItems: ArrayBuffer[ResourceItem] = {
        val
    }

}

object ResourceFolderMaintainer {
    val MaintainerJSONFileName: String = "maintainer.json"
}
