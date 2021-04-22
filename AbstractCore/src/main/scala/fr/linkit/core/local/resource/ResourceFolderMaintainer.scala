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

import fr.linkit.api.local.resource._
import fr.linkit.api.local.resource.representation.{ResourceFolder, ResourceRepresentation}
import fr.linkit.api.local.system.fsa.FileSystemAdapter
import fr.linkit.core.local.resource.ResourceFolderMaintainer.{MaintainerFileName, Resources, loadResources}
import fr.linkit.core.local.system.AbstractCoreConstants.{UserGson => Gson}
import fr.linkit.core.local.system.{DynamicVersions, StaticVersions}

import scala.collection.mutable.ArrayBuffer

class ResourceFolderMaintainer(maintained: ResourceFolder, listener: ResourceListener, fsa: FileSystemAdapter) extends ResourcesMaintainer with ResourcesMaintainerInformer {

    private   val maintainerFileAdapter = fsa.getAdapter(maintained.getAdapter.getAbsolutePath + "/" + MaintainerFileName)
    protected val resources: Resources  = loadResources(fsa, maintained)

    override def getResources: ResourceFolder = maintained

    override def getBehaviors: Array[AutomaticBehaviorOption] = {
        println(s"resources.options = ${resources.options}")
        resources.options.toArray
    }

    override def setBehaviors(behaviorOptions: Seq[AutomaticBehaviorOption]): Unit = {
        val options = resources.options
        options.clear()
        options.addAll(behaviorOptions)
        listener.putMaintainer(this)
        updateFile()
    }

    override def isRemoteResource(name: String): Boolean = isKnown(name) && !maintained.isPresentOnDrive(name)

    override def isKnown(name: String): Boolean = name == MaintainerFileName || resources.get(name).isDefined

    override def getLastChecksum(name: String): Long = {
        resources
                .get(name)
                .map(_.lastChecksum)
                .getOrElse {
                    if (name == maintained.name) {
                        resources.folder.lastChecksum
                    } else {
                        throw NoSuchResourceException(s"Resource $name is unknown for folder resource ${maintained.getLocation}")
                    }
                }
    }

    override def getLastModified(name: String): DynamicVersions = {
        resources
                .get(name)
                .map(_.lastModified)
                .getOrElse {
                    if (maintained.isPresentOnDrive(name)) {
                        DynamicVersions.unknown
                    } else if (name == maintained.name) {
                        resources.folder.lastModified
                    } else {
                        throw NoSuchResourceException(s"Resource $name is unknown for folder resource ${maintained.getLocation}")
                    }
                }
    }

    override def informLocalModification(name: String): Unit = {
        println(s"LocalModInformed ! '$name' in folder ${maintained.getLocation}")
    }

    @throws[ResourceAlreadyPresentException]
    private[resource] def registerResource(resource: ResourceRepresentation, localOnly: Boolean): Unit = {
        if (isKnown(resource.name))
            throw ResourceAlreadyPresentException("This source is already known !")

        if (resource.getParent.ne(maintained))
            throw new IllegalArgumentException("Given resource's parent folder is not handled by this maintainer.")

        val item = ResourceItem(resource.name)
        item.isLocalOnly = localOnly
        item.lastChecksum = resource.getChecksum
        item.lastModified = DynamicVersions.from(StaticVersions.currentVersion)

        resources += item
        updateFile()
    }

    private def updateFile(): Unit = {
        println(s"Saving resources = ${resources}")
        val json = Gson.toJson(resources)
        val out  = maintainerFileAdapter.newOutputStream()
        out.write(json.getBytes())
        out.close()
    }

}

object ResourceFolderMaintainer {

    val MaintainerFileName: String = "maintainer.json"

    private def loadResources(fsa: FileSystemAdapter, maintained: ResourceFolder): Resources = {
        val maintainerPath        = maintained.getAdapter.getAbsolutePath + "/" + MaintainerFileName
        val maintainerFileAdapter = fsa.getAdapter(maintainerPath)
        if (maintainerFileAdapter.notExists) {
            maintainerFileAdapter.createAsFile()
            return Resources(ResourceItem.minimal(maintained, true), ArrayBuffer.empty)
        }
        val json      = maintainerFileAdapter.getContentString
        val resources = Gson.fromJson(json, classOf[Resources])
        if (resources == null)
            return Resources(ResourceItem.minimal(maintained, true), ArrayBuffer.empty)

        def handleItem(item: ResourceItem): Unit = {
            val name    = item.name
            val adapter = fsa.getAdapter(maintainerFileAdapter.getAbsolutePath + "/" + name)
            if (adapter.notExists) {
                resources -= item
                return
            }

            if (adapter.isDirectory) {
                val dirResource        = maintained.registerFolder(name, item.isLocalOnly)
                val dirResourceOptions = loadResources(fsa, dirResource).options.toSeq

                dirResource.getMaintainer
                        .setBehaviors(dirResourceOptions)

            } else {
                maintained.registerFile(name, item.isLocalOnly)
            }
        }

        resources.foreach(handleItem)

        resources
    }

    case class Resources(folder: ResourceItem, options: ArrayBuffer[AutomaticBehaviorOption]) {

        private val children: ArrayBuffer[ResourceItem] = ArrayBuffer.empty

        def get(name: String): Option[ResourceItem] = children.find(_.name == name)

        def foreach(action: ResourceItem => Unit): Unit = children.clone().foreach(action)

        def +=(item: ResourceItem): Unit = children += item

        def -=(item: ResourceItem): Unit = children -= item
    }

}
