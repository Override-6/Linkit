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

import fr.linkit.api.local.resource.ResourceListener
import fr.linkit.api.local.resource.exception.{IncompatibleResourceTypeException, _}
import fr.linkit.api.local.resource.external._
import fr.linkit.api.local.system.fsa.FileAdapter
import fr.linkit.core.local.resource.ResourceFolderMaintainer
import fr.linkit.core.local.resource.entry.LocalResourceFolder.checkResourceName
import org.jetbrains.annotations.NotNull

import java.io.File
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class LocalResourceFolder private(adapter: FileAdapter,
                                  listener: ResourceListener,
                                  parent: ResourceFolder) extends AbstractResource(parent, adapter.createAsFolder()) with ResourceFolder with LocalExternalResource {

    println(s"Creating resource folder $getLocation...")

    private val fsa        = adapter.getFSAdapter
    private val resources  = new mutable.HashMap[String, ExternalResource]()
    private val maintainer = this.synchronized {
        new ResourceFolderMaintainer(this, listener, fsa)
    }

    protected val entry = new DefaultResourceEntry[ResourceFolder](this)

    override def getEntry: ResourceEntry[ResourceFolder] = entry

    override def getMaintainer: ResourceFolderMaintainer = maintainer

    override def createOnDisk(): Unit = getAdapter.createAsFolder()

    @throws[NoSuchResourceException]("If no resource was found with the provided name")
    @throws[IncompatibleResourceTypeException]("If a resource was found but with another type than R.")
    @NotNull
    override def get[R <: ExternalResource : ClassTag](name: String): R = {
        resources
                .get(name)
                .fold(throw NoSuchResourceException(s"Resource $name not registered in resource folder '$getLocation'")) {
                    case resource: R => resource
                    case other       => throw IncompatibleResourceTypeException(s"Requested resource of type '${classTag[R].runtimeClass.getSimpleName}' but found resource '${other.getClass.getSimpleName}'")
                }
    }

    override def find[R <: ExternalResource : ClassTag](name: String): Option[R] = {
        resources
                .get(name)
                .flatMap {
                    case resource: R => Some(resource)
                    case _           => None
                }
    }

    override def getChecksum: Long = {
        resources.values
                .map(_.getChecksum)
                .sum
    }

    override def getLastChecksum: Long = {
        maintainer.getLastChecksum(name)
    }

    override def unregister(name: String): Unit = {
        resources -= name
        maintainer.unregisterResource(name)
    }

    @throws[IllegalResourceException]("If the provided name contains invalid character")
    override def register[R <: ExternalResource](name: String, factory: ExternalResourceFactory[R]): R = this.synchronized {
        checkResourceName(name)

        val resourcePath = getAdapter.getAbsolutePath + File.separator + name
        val adapter      = fsa.getAdapter(resourcePath)
        val resource     = factory(adapter, listener, this)

        resources.put(name, resource)
        //If maintainer is null, that's mean that this method is called during it's initialization.
        //And, therefore the maintainer of this folder have invoked this method.
        //Thus the registration is partially handled by the maintainer and just want
        //the ResourceFolder to add the resource in it's memory.
        //The execution of "maintainer.registerResource(resource)" will be manually handled by the maintainer.
        if (maintainer != null)
            maintainer.registerResource(resource)
        resource
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
    override def openResource[R <: ExternalResource : ClassTag](name: String, factory: ExternalResourceFactory[R]): R = {
        ensureResourceOpenable(name)
        println(s"Opening resource $name, ${classTag[R].runtimeClass}")

        register(name, factory)
    }

    private def ensureResourceOpenable(name: String): Unit = {
        checkResourceName(name)

        val adapter = fsa.getAdapter(getAdapter.getAbsolutePath + "/" + name)
        if (adapter.exists)
            throw ResourceAlreadyPresentException("The requested resource already exists on this machine's drive.")
    }

}

object LocalResourceFolder extends ExternalResourceFactory[ResourceFolder] {

    override def apply(adapter: FileAdapter,
                       listener: ResourceListener,
                       parent: ResourceFolder): ResourceFolder = {
        new LocalResourceFolder(adapter, listener, parent)
    }

    val ForbiddenChars: Array[Char] = Array('\\', '/', ':', '?', '"', '<', '>', '|')

    private def checkResourceName(name: String): Unit = {
        name.exists(ForbiddenChars.contains)
    }

}
