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
import fr.linkit.api.local.system.fsa.FileSystemAdapter
import fr.linkit.core.local.resource.ResourceFolderMaintainer.{MaintainerFileName, Resources}
import fr.linkit.core.local.system.{AbstractCoreConstants, DynamicVersions}

import scala.collection.mutable.ArrayBuffer

class ResourceFolderMaintainer(maintained: ExternalResourceFolder, fsa: FileSystemAdapter) extends ResourcesMaintainer with ResourcesMaintainerInformer {

    protected val gson     : Gson       = AbstractCoreConstants.UserGson
    private   val maintainerFileAdapter = fsa.getAdapter(maintained.getLocation + "/" + MaintainerFileName)
    protected val resources: Resources  = loadResources

    override def getResources: ExternalResourceFolder = maintained

    override def isRemoteResource(name: String): Boolean = isKnown(name) && !maintained.isPresentOnDrive(name)

    override def isKnown(name: String): Boolean = name == MaintainerFileName || resources.children.exists(_.name == name)

    override def getLastChecksum(name: String): Long = {
        resources
                .children
                .find(_.name == name)
                .map(_.lastChecksum)
                .getOrElse {
                    if (name == maintained.name) {
                        resources.folder.lastChecksum
                    } else {
                        throw new NoSuchResourceException(s"Resource $name is unknown for folder resource ${maintained.getLocation}")
                    }
                }
    }

    override def getLastModified(name: String): DynamicVersions = {
        println(s"name = ${name}")
        println(s"resources = ${resources}")
        resources
                .children
                .find(_.name == name)
                .map(_.lastModified)
                .getOrElse {
                    if (maintained.isPresentOnDrive(name)) {
                        DynamicVersions.unknown
                    } else if (name == maintained.name) {
                        resources.folder.lastModified
                    } else {
                        throw new NoSuchResourceException(s"Resource $name is unknown for folder resource ${maintained.getLocation}")
                    }
                }
    }

    override def informLocalModification(name: String): Unit = {
        println(s"LocalModInformed ! '$name' in folder ${maintained.getLocation}")
    }

    private[resource] def registerResource(resource: ExternalResource, localOnly: Boolean): Unit = {
        if (isKnown(resource.name))
            throw new IllegalArgumentException("This source is already known !")

        println(s"Registering resource ${resource.getAdapter}")
        if (resource.getParent.ne(maintained))
            throw new IllegalArgumentException("Given resource's parent folder is not handled by this maintainer.")

        val item = ResourceItem(resource.name)
        item.isLocalOnly = localOnly
        item.lastChecksum = resource.getChecksum
        item.lastModified = getLastModified(resource.name)

        resources.children += item
        updateFile()
    }

    private def updateFile(): Unit = {
        println(s"Saving resources = ${resources}")
        println(s"Saving resources as array : ${resources.children.toArray.mkString("Array(", ", ", ")")}")
        val json = gson.toJson(resources)
        println(s"json = ${json}")
        val out = maintainerFileAdapter.newOutputStream()
        out.write(json.getBytes())
        out.close()
    }

    private def loadResources: Resources = {
        if (maintainerFileAdapter.notExists) {
            maintainerFileAdapter.create()
            return Resources(ResourceItem(maintained, true))
        }
        val json      = maintainerFileAdapter.getContentString
        val resources = gson.fromJson(json, classOf[Resources])
        if (resources == null)
            return Resources(ResourceItem(maintained, true))

        resources
    }

}

object ResourceFolderMaintainer {

    val MaintainerFileName: String = "maintainer.json"

    case class Resources(folder: ResourceItem) {

        val children: ArrayBuffer[ResourceItem] = ArrayBuffer.empty
    }

}
