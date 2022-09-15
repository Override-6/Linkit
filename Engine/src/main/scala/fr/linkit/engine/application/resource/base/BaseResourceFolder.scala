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

package fr.linkit.engine.application.resource.base

import fr.linkit.api.application.resource.exception.{IllegalResourceException, IncompatibleResourceTypeException, NoSuchResourceException, ResourceAlreadyPresentException}
import fr.linkit.api.application.resource.external.{Resource, ResourceEntry, ResourceFactory, ResourceFolder}
import fr.linkit.api.application.resource.{ResourceListener, ResourcesMaintainer}
import fr.linkit.engine.application.resource.ResourceFolderMaintainer
import fr.linkit.engine.application.resource.external.DefaultResourceEntry
import fr.linkit.engine.application.resource.external.LocalResourceFolder.ForbiddenChars
import org.jetbrains.annotations.{NotNull, Nullable}

import java.io.File
import java.nio.file.{Files, Path}
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

abstract class BaseResourceFolder(@Nullable parent: ResourceFolder, listener: ResourceListener, adapter: Path) extends AbstractResource(parent, adapter) with ResourceFolder {

    protected val resources                                 = new mutable.HashMap[String, Resource]()
    protected val entry     : ResourceEntry[ResourceFolder] = new DefaultResourceEntry[ResourceFolder](this)
    protected val maintainer: ResourceFolderMaintainer      = this.synchronized {
        new ResourceFolderMaintainer(this, listener)
    }

    override def getMaintainer: ResourcesMaintainer = maintainer

    override def getChecksum: Long = resources
            .map(_._2.getChecksum)
            .sum

    override def close(): Unit = {
        resources.foreachEntry((_, resource) => resource.close())
        entry.close()
    }

    override def getEntry: ResourceEntry[ResourceFolder] = entry

    override def getLastChecksum: Long = {
        maintainer.getLastChecksum(name)
    }

    @throws[ResourceAlreadyPresentException]("If the subPath targets a resource that is already registered or opened.")
    @throws[IllegalResourceException]("If the provided name contains invalid character")
    override def openResource[R <: Resource : ClassTag](name: String, factory: ResourceFactory[R]): R = {
        ensureResourceOpenable(name)

        register(name, factory)
    }

    override def isPresentOnDrive(name: String): Boolean = Files.exists(Path.of(getPath.toString + File.separator + name))

    @throws[NoSuchResourceException]("If no resource was found with the provided name")
    @throws[IncompatibleResourceTypeException]("If a resource was found but with another type than R.")
    @NotNull
    override def get[R <: Resource : ClassTag](name: String): R = {
        resources
                .get(name)
                .fold(throw NoSuchResourceException(s"Resource $name not registered in resource folder '$getLocation'")) {
                    case resource: R => resource
                    case other       => throw IncompatibleResourceTypeException(s"Requested resource of type '${classTag[R].runtimeClass.getSimpleName}' but found resource '${other.getClass.getSimpleName}'")
                }
    }

    override def getOrOpen[R <: Resource : ClassTag](name: String)(implicit factory: ResourceFactory[R]): R = {
        find[R](name).getOrElse {
            if (isPresentOnDrive(name))
                register[R](name, factory)
            else openResource[R](name, factory)
        }
    }

    override def find[R <: Resource : ClassTag](name: String): Option[R] = {
        resources
                .get(name)
                .flatMap {
                    case resource: R => Some(resource)
                    case _           => None
                }
    }

    private def ensureResourceOpenable(name: String): Unit = {
        checkResourceName(name)

        if (Files.exists(Path.of(name)))
            throw ResourceAlreadyPresentException("The requested resource already exists on this machine's drive.")
    }

    @throws[IllegalResourceException]("If the provided name contains invalid character")
    override def register[R <: Resource](name: String, factory: ResourceFactory[R]): R = this.synchronized {
        checkResourceName(name)

        val resourcePath = getPath + File.separator + name
        val adapter      = Path.of(resourcePath)
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

    override def unregister(name: String): Unit = {
        resources -= name
        maintainer.unregisterResource(name)
    }

    protected def checkResourceName(name: String): Unit = {
        if (name == null)
            throw new NullPointerException
        if (name.exists(ForbiddenChars.contains))
            throw new IllegalResourceException(s"Provided source name is invalid. ($name)")
    }
}
