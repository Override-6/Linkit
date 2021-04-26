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

package fr.linkit.core.local.resource.base

import fr.linkit.api.local.resource.exception.{IllegalResourceException, IncompatibleResourceTypeException, NoSuchResourceException, ResourceAlreadyPresentException}
import fr.linkit.api.local.resource.external.{ExternalResource, ExternalResourceFactory, ResourceEntry, ResourceFolder}
import fr.linkit.api.local.resource.{ResourceListener, ResourcesMaintainer}
import fr.linkit.api.local.system.fsa.FileAdapter
import fr.linkit.core.local.resource.ResourceFolderMaintainer
import fr.linkit.core.local.resource.local.DefaultResourceEntry
import fr.linkit.core.local.resource.local.LocalResourceFolder.ForbiddenChars
import org.jetbrains.annotations.NotNull

import java.io.File
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class AbstractResourceFolder(parent: ResourceFolder, listener: ResourceListener, adapter: FileAdapter) extends AbstractResource(parent, adapter) with ResourceFolder {

    private   val resources  = new mutable.HashMap[String, ExternalResource]()
    private   val fsa        = adapter.getFSAdapter
    protected val entry      = new DefaultResourceEntry[ResourceFolder](this)
    private   val maintainer = this.synchronized {
        new ResourceFolderMaintainer(this, listener)
    }

    override protected def getMaintainer: ResourcesMaintainer = maintainer

    override def getChecksum: Long = ???

    override def close(): Unit = ???

    override def getEntry: ResourceEntry[ResourceFolder] = entry

    /**
     * Opens a resource folder located under this folder's path.
     * @param name the relative path in which the resource will be stored.
     * @tparam R the kind of resource that must be opened.
     * @return an instance of [[R]], which is the resource file.
     * @throws ResourceAlreadyPresentException if name is already registered for the resource folder.
     * @throws IllegalResourceException If the provided name contains invalid character.
     * */
    override def openResource[R <: ExternalResource : ClassTag](name: String, factory: ExternalResourceFactory[R]): R = ???

    /**
     * No matter if a file is registered by the maintainer or not, this method
     * must return true if any file or folder with the provided name is stored into
     * the handled folder.
     *
     * @param name the file/folder name to test.
     * @return {{{true}}} if any file/folder is stored on the current machine, {{{false}}} instead.
     * */
    override def isPresentOnDrive(name: String): Boolean = ???

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

    @throws[IllegalResourceException]("If the provided name contains invalid character")
    override def register[R <: ExternalResource](name: String, factory: ExternalResourceFactory[R]): R = this.synchronized {
        checkResourceName(name)

        if (isKnown(name))
            throw ResourceAlreadyPresentException("This resource is already registered.")

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

    /**
     * Unregisters a resource.
     * This method takes no effect if the provided resource's name is unknown.
     *
     * @param name the resource name to unregister.
     * */
    override def unregister(name: String): Unit = ???

    /**
     * Performs a non-recursive scan of all the content of this folder.
     * Each times the scan hits a resource that is not yet registered, the scanAction gets called.
     * scanAction may determine whether the hit resource must be registered or not, attached by
     * any representation kind, or destroyed...
     *
     * The implementation can perform default operations before or after invoking the scanAction.
     *
     * @param scanAction the action to perform on each new resource.
     * */
    override def scan(scanAction: String => Unit): Unit = ???

    protected def checkResourceName(name: String): Unit = {
        name.exists(ForbiddenChars.contains)
    }
}
