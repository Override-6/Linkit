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

package linkit.base.resource

import fr.linkit.api.application.resource._
import fr.linkit.api.application.resource.exception.NoSuchResourceException
import fr.linkit.api.application.resource.local.{Resource, ResourceFolder}
import linkit.base.resource.ResourceFolderMaintainer.{MaintainerFileName, Resources}
import linkit.base.resource.local.LocalResourceFactories
import fr.linkit.engine.internal.system.EngineConstants.{UserGson => Gson}
import fr.linkit.engine.internal.system.{DynamicVersions, StaticVersions}

import java.nio.file.{Files, Path}
import java.util

class ResourceFolderMaintainer(maintained: ResourceFolder,
                               listener: ResourceListener) extends ResourcesMaintainer {

    private   val maintainerFileAdapter = Path.of(maintained.getPath.toString + "/" + MaintainerFileName)
    protected val resources: Resources  = loadResources()
    listener.putMaintainer(this, MaintainerKey)

    override def getResources: ResourceFolder = maintained

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

    private[linkit] def unregisterResource(name: String): Unit = {
        resources -= name
    }

    private[linkit] def registerResource(resource: Resource): Unit = {
        if (resource.getParent.exists(_.ne(maintained)))
            throw new IllegalArgumentException("Given resource's parent folder is not handled by this maintainer.")

        if (resources.get(resource.name).isDefined)
            return

        val item = ResourceItem(resource.name)
        item.lastChecksum = resource.getChecksum
        item.lastModified = DynamicVersions.from(StaticVersions.currentVersions)
        //println(s"Registered item $item")

        if (resources.get(item.name).exists(_.lastChecksum == item.lastChecksum)) {
            return
        }

        resources put item
        updateFile()
    }

    private def updateFile(resources: Resources = this.resources): Unit = try {
        if (Files.notExists(maintainerFileAdapter))
            Files.createFile(maintainerFileAdapter)
        //println(s"Saving resources for folder : ${resources.folder}")
        val json = Gson.toJson(resources)
        val out  = Files.newOutputStream(maintainerFileAdapter)
        out.write(json.getBytes())
        out.close()
    }

    object MaintainerKey extends ResourceKey {

        override def onModify(name: String): Unit = runIfKnown(name) { (resource, item) =>
            if (!item.lastModified.sameVersions(StaticVersions.currentVersions)) {
                item.lastModified.setAll(StaticVersions.currentVersions)
            }

            def itemChecksum: Long = item.lastChecksum

            val itemFolder = resources.folder
            itemFolder.lastChecksum -= itemChecksum
            item.lastChecksum = resource.getChecksum
            itemFolder.lastChecksum += itemChecksum

            updateFile()
            //println(s"item = ${item}")
        }

        override def onDelete(name: String): Unit = runIfKnown(name) { (_, _) =>
            maintained.unregister(name)
            //println(s"Unregistered $name")
            updateFile()
        }

        override def onCreate(name: String): Unit = {
            if (isKnown(name))
                return
            maintained.register(name, LocalResourceFactories.adaptive)
            //println(s"Registered $name")
            updateFile()
        }

        private def runIfKnown(name: String)(callback: (Resource, ResourceItem) => Unit): Unit = {
            val resource = maintained.find[Resource](name)
            val item     = resources.get(name)

            if (resource.isEmpty && item.isEmpty)
                return
            if (resource.isDefined && item.isEmpty) {
                if (maintained.isPresentOnDrive(name)) {
                    registerResource(resource.get)
                } else {
                    maintained.unregister(name)
                }
                return
            }
            if (item.isDefined && resource.isEmpty) {
                if (maintained.isPresentOnDrive(name)) {
                    maintained.register(name, LocalResourceFactories.adaptive)
                } else {
                    unregisterResource(name)
                }
                return
            }

            callback(resource.get, item.get)
        }
    }

    def loadResources(): Resources = {
        val maintainerPath     = maintained.getPath.toString + "/" + MaintainerFileName
        val maintainerFilePath = Path.of(maintainerPath)
        if (Files.notExists(maintainerFilePath)) {
            Files.createDirectories(maintainerFilePath.getParent)
            Files.createFile(maintainerFilePath)
            val resources = Resources(ResourceItem.minimal(maintained))
            updateFile(resources)
            return resources
        }
        val json      = Files.readString(maintainerFilePath)
        val resources = Gson.fromJson(json, classOf[Resources])
        if (resources == null) {
            val resources = Resources(ResourceItem.minimal(maintained))
            updateFile(resources)
            return resources
        }

        var modified = false

        def handleItem(item: ResourceItem): Unit = {
            val name    = item.name
            val adapter = Path.of(maintained.getPath.toString + "/" + name)
            if (Files.notExists(adapter)) {
                resources -= item
                modified = true
                return
            }

            //println(s"handling item = ${item}")
            maintained.register(name, LocalResourceFactories.adaptive)
        }

        resources.foreach(handleItem)
        if (modified)
            updateFile(resources)

        resources
    }

}

object ResourceFolderMaintainer {

    val MaintainerFileName: String = "maintainer.json"

    case class Resources(folder: ResourceItem) {

        private val children: util.HashMap[String, ResourceItem] = new util.HashMap()

        def get(name: String): Option[ResourceItem] = {
            Option(children.get(name))
        }

        def foreach(action: ResourceItem => Unit): Unit = {
            children.values.toArray(Array[ResourceItem]()).foreach(action)
        }

        def put(item: ResourceItem): Unit = children.put(item.name, item)

        def -=(item: ResourceItem): Unit = children.remove(item.name)

        def -=(itemName: String): Unit = children.remove(itemName)

        override def toString: String = s"Resources(folder: $folder, children: ${children.values()})"
    }

}
