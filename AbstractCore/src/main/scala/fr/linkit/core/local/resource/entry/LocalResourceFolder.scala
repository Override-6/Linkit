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

package fr.linkit.core.local.resource.entry

import fr.linkit.api.local.resource._
import fr.linkit.api.local.resource.external.{ExternalResource, ResourceFile, ResourceFolder}
import fr.linkit.api.local.resource.exception._
import fr.linkit.api.local.system.fsa.{FileAdapter, FileSystemAdapter}
import fr.linkit.core.local.resource.entry.LocalResourceFolder.checkResourceName
import fr.linkit.core.local.resource.{ResourceFolderMaintainer, ResourceListener}
import org.jetbrains.annotations.NotNull

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class LocalResourceFolder private(fsa: FileSystemAdapter,
                                  path: String,
                                  listener: ResourceListener,
                                  parent: ResourceFolder) extends AbstractResource(parent, fsa.getAdapter(path).createAsFolder()) with ResourceFolder {

    println(s"Creating resource folder $path...")

    private val resources = new mutable.HashMap[String, DefaultResourceEntry]()

    private lazy val maintainer = new ResourceFolderMaintainer(this, listener, fsa)

    override def getMaintainer: ResourceFolderMaintainer = maintainer

    @throws[NoSuchResourceException]("If no resource was found with the provided name")
    @throws[NoSuchRepresentationException]("If a resource was found but with another type than R.")
    @NotNull
    override def get[R <: ExternalResource : ClassTag](name: String): R = {
        resources
                .get(name)
                .fold(throw NoSuchResourceException(s"Resource $name not registered in resource folder '$getLocation'")) { res =>
                    res.getRepresentation[R]
                }
    }

    override def find[R <: ExternalResource : ClassTag](name: String): Option[R] = {
        resources.get(name).flatMap(_.findRepresentation[R])
    }

    override def getChecksum: Long = {
        resources.values
                .map(_.resource.getChecksum)
                .sum
    }

    override def getLastChecksum: Long = {
        resources.values
                .map(_.resource.getLastChecksum)
                .sum
    }

    override def unregister(name: String): Unit = {
        resources -= name
        maintainer.unregisterResource(name)
    }

    override def register(name: String): ExternalResource = {
        checkResourceName(name)

        val resourcePath = getAdapter.getAbsolutePath + "/" + name
        val adapter      = fsa.getAdapter(resourcePath)
        if (adapter.notExists)
            throw NoSuchResourceException(s"Resource $resourcePath not found.")

        val defaultResource = if (adapter.isDirectory) {
            new LocalResourceFolder(fsa, resourcePath, listener, this)
        } else {
            new LocalResourceFile(this, adapter)
        }
        resources.put(name, new DefaultResourceEntry(defaultResource))
        maintainer.registerResource(defaultResource)
        get[ExternalResource](name)
    }

    override def scan(scanAction: String => Unit): Unit = {
        fsa.list(getAdapter)
                .map(_.getName)
                .filterNot(maintainer.isKnown)
                .foreach(scanAction)
    }

    override def isPresentOnDrive(name: String): Boolean = fsa.exists(getAdapter.getAbsolutePath + "/" + name)

    @throws[ResourceAlreadyPresentException]("If the subPath targets a resource that is already registered or opened.")
    @throws[IllegalResourceException]("If the provided name contains invalid character")
    override def openResourceFile(name: String): ResourceFile = {
        val adapter = ensureResourceOpenable(name)

        adapter.createAsFile()
        register(name)
        get[ResourceFile](name)
    }

    @throws[ResourceAlreadyPresentException]("If the subPath targets a resource that is already registered or opened.")
    @throws[IllegalResourceException]("If the provided name contains invalid character")
    override def openResourceFolder(name: String): ResourceFolder = {
        val adapter = ensureResourceOpenable(name)

        adapter.createAsFolder()
        register(name)
        get[ResourceFolder](name)
    }

    private def ensureResourceOpenable(name: String): FileAdapter = {
        checkResourceName(name)

        val adapter = fsa.getAdapter(getAdapter.getAbsolutePath + "/" + name)
        if (adapter.exists)
            throw ResourceAlreadyPresentException("The requested resource already exists on this machine's drive.")

        adapter
    }

}

object LocalResourceFolder {

    def apply(fsa: FileSystemAdapter,
              path: String,
              listener: ResourceListener,
              parent: ResourceFolder): LocalResourceFolder = {
        new LocalResourceFolder(fsa, path, listener, parent)
    }

    val ForbiddenChars: Array[Char] = Array('\\', '/', ':', '?', '"', '<', '>', '|')

    private def checkResourceName(name: String): Unit = {
        name.exists(ForbiddenChars.contains)
    }

}
