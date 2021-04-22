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

import fr.linkit.api.local.resource.representation.{ResourceFile, ResourceFolder, ResourceRepresentation, ResourceRepresentationFactory}
import fr.linkit.api.local.resource.{ResourceAlreadyPresentException, _}
import fr.linkit.api.local.system.fsa.{FileAdapter, FileSystemAdapter}
import fr.linkit.core.local.resource.DefaultResourceFolder.checkResourceName
import org.jetbrains.annotations.NotNull

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class DefaultResourceFolder private(fsa: FileSystemAdapter,
                                    path: String,
                                    listener: ResourceListener,
                                    parent: ResourceFolder) extends AbstractResourceRepresentation(parent, fsa.getAdapter(path).createAsFolder()) with ResourceFolder {

    println(s"Creating resource folder $path...")

    private val resources = new mutable.HashMap[String, DefaultResourceEntry]()

    private lazy val maintainer = new ResourceFolderMaintainer(this, listener, fsa)
    listener.putMaintainer(maintainer)

    override def getMaintainer: ResourceFolderMaintainer = maintainer

    @throws[NoSuchResourceException]("If no resource was found with the provided name")
    @throws[NoSuchRepresentationException]("If a resource was found but with another type than R.")
    @NotNull
    override def get[R <: ResourceRepresentation : ClassTag](name: String): R = {
        resources
                .get(name)
                .fold(throw NoSuchResourceException(s"Resource $name not registered in resource folder '$getLocation'")) { res =>
                    res.getRepresentation[R]
                }
    }

    override def find[R <: ResourceRepresentation : ClassTag](name: String): Option[R] = {
        resources.get(name).flatMap(_.findRepresentation[R])
    }

    override def getChecksum: Long = {
        resources.values
                .map(_.defaultRepresentation.getChecksum)
                .sum
    }

    override def getLastChecksum: Long = {
        resources.values
                .map(_.defaultRepresentation.getLastChecksum)
                .sum
    }

    override def registerFile(name: String, localOnly: Boolean): ResourceFile = {
        register(name, localOnly, Seq.empty)
        get[ResourceFile](name)
    }

    override def registerFolder(name: String, localOnly: Boolean, options: AutomaticBehaviorOption*): ResourceFolder = {
        register(name, localOnly, options)
        get[ResourceFolder](name)
    }

    override def attachRepresentation[R <: ResourceRepresentation : ClassTag](name: String, factory: ResourceRepresentationFactory[R]): Unit = {

        def abort(requested: String, found: String): Unit =
            throw new IncompatibleResourceTypeException(s"Attempted to attach a $requested resource representation to a $found.")

        resources
                .get(name)
                .fold(throw NoSuchResourceException(s"Resource $name not found in folder $getLocation.")) { res =>
                    val rClass = classTag[R].runtimeClass
                    res.defaultRepresentation match {
                        case _: ResourceFile   =>
                            if (!classOf[ResourceFile].isAssignableFrom(rClass))
                                abort("folder", "file")
                        case _: ResourceFolder =>
                            if (!classOf[ResourceFolder].isAssignableFrom(rClass))
                                abort("file", "folder")
                    }

                    res.attachRepresentation(factory)
                }

    }

    override def scan(scanAction: String => Unit): Unit = {
        fsa.list(getAdapter)
                .map(_.getName)
                .filterNot(maintainer.isKnown)
                .foreach(scanAction)

    }

    override def isPresentOnDrive(name: String): Boolean = fsa.exists(getLocation + "/" + name)

    @throws[ResourceAlreadyPresentException]("If the subPath targets a resource that is already registered or opened.")
    @throws[IllegalResourceException]("If the provided name contains invalid character")
    override def openResourceFile(name: String, localOnly: Boolean): ResourceFile = {
        val adapter = ensureResourceOpenable(name)

        adapter.createAsFile()
        registerFile(name, localOnly)
        get[ResourceFile](name)
    }

    @throws[ResourceAlreadyPresentException]("If the subPath targets a resource that is already registered or opened.")
    @throws[IllegalResourceException]("If the provided name contains invalid character")
    override def openResourceFolder(name: String, localOnly: Boolean, behaviorOptions: AutomaticBehaviorOption*): ResourceFolder = {
        val adapter = ensureResourceOpenable(name)

        adapter.createAsFolder()
        registerFolder(name, localOnly)
        get[ResourceFolder](name)
    }

    private def register(name: String, localOnly: Boolean, options: Seq[AutomaticBehaviorOption]): Unit = {
        ensureResourceRegistrable(name)

        val resourcePath = getAdapter.getAbsolutePath + "/" + name
        val adapter      = fsa.getAdapter(resourcePath)
        if (adapter.notExists)
            throw NoSuchResourceException(s"Resource $resourcePath not found.")

        val defaultResource = if (adapter.isDirectory) {
            DefaultResourceFolder(fsa, resourcePath, listener, this, options)
        } else {
            new DefaultResourceFile(this, adapter)
        }
        resources.put(name, new DefaultResourceEntry(defaultResource))
        maintainer.registerResource(defaultResource, localOnly)
    }

    private def ensureResourceRegistrable(name: String): Unit = {
        checkResourceName(name)
        if (maintainer.isKnown(name))
            throw ResourceAlreadyPresentException(name)
    }

    private def ensureResourceOpenable(name: String): FileAdapter = {
        ensureResourceRegistrable(name)

        val adapter = fsa.getAdapter(getAdapter.getAbsolutePath + "/" + name)
        if (adapter.exists)
            throw ResourceAlreadyPresentException("The requested resource already exists on this machine's drive.")

        adapter
    }

}

object DefaultResourceFolder {

    def apply(fsa: FileSystemAdapter,
              path: String,
              listener: ResourceListener,
              parent: ResourceFolder,
              options: Seq[AutomaticBehaviorOption] = Seq.empty): DefaultResourceFolder = {
        val resources = new DefaultResourceFolder(fsa, path, listener, parent)
        resources.getMaintainer.setBehaviors(options)
        resources
    }

    val ForbiddenChars: Array[Char] = Array('\\', '/', ':', '?', '"', '<', '>', '|')

    private def checkResourceName(name: String): Unit = {
        name.exists(ForbiddenChars.contains)
    }

}
