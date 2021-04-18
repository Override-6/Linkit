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
import fr.linkit.api.local.system.fsa.{FileAdapter, FileSystemAdapter}
import fr.linkit.core.local.system.{DynamicVersions, StaticVersions}

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class DefaultExternalResourceFolder private(fsa: FileSystemAdapter,
                                            path: String,
                                            listener: ResourceListener,
                                            parent: ExternalResourceFolder) extends ExternalResourceFolder {

    println(s"Creating resource folder $path...")

    private val location   = fsa.getAdapter(path).create()
    private val resources  = new mutable.HashMap[String, ResourceRepresentations]()
    private val maintainer = new ResourceFolderMaintainer(this, listener, fsa)

    override val name: String = location.getName
    private  val lastModified = maintainer.getLastModified(name)

    listener.putMaintainer(maintainer)

    override def getMaintainer: ResourceFolderMaintainer = maintainer

    override def getLocation: String = location.getAbsolutePath

    override def getParent: ExternalResourceFolder = parent

    override def getAdapter: FileAdapter = location

    override def get[R <: ExternalResource : ClassTag](name: String): R = {
        resources
            .get(name)
            .fold(throw new NoSuchResourceException(s"Resource $name not registered in resource folder '$getLocation'")) { res =>
                res
                    .get[R]
                    .getOrElse(throw new BadResourceTypeException(s"No factory of resource representation '${classTag[R].runtimeClass} was registered for resource ${res.path}"))
            }
    }

    override def find[R <: ExternalResource : ClassTag](name: String): Option[R] = {
        resources.get(name).flatMap(_.get[R])
    }

    override def getLastModified: DynamicVersions = lastModified

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

    override def markAsModifiedByCurrentApp(): Unit = {
        lastModified.setAll(StaticVersions.currentVersion)
        if (getParent != null)
            getParent.markAsModifiedByCurrentApp()
    }

    override def register(name: String, localOnly: Boolean, options: AutomaticBehaviorOption*): Unit = {
        val resourcePath = getLocation + "/" + name
        val adapter      = fsa.getAdapter(getLocation + "/" + name)
        if (adapter.notExists)
            throw new NoSuchResourceException(s"Resource $resourcePath not found.")

        val defaultResource = if (adapter.isDirectory) {
            DefaultExternalResourceFolder(fsa, resourcePath, listener, this, options)
        } else {
            new DefaultExternalResourceFile(this, adapter)
        }
        resources.put(name, new ResourceRepresentations(defaultResource))
        maintainer.registerResource(defaultResource, localOnly)
    }

    override def attachFactory[R <: ExternalResource : ClassTag](name: String, factory: ResourceFactory[R]): Unit = {
        def abort(requested: String, found: String): Unit =
            throw new IncompatibleResourceTypeException(s"Attempted to attach a $requested resource representation to a $found.")

        resources
            .get(name)
            .fold(throw new NoSuchResourceException(s"Resource $name not found in folder $getLocation.")) { res =>
                val rClass = classTag[R].runtimeClass
                res.resource match {
                    case _: ExternalResourceFile   =>
                        if (!classOf[ExternalResourceFile].isAssignableFrom(rClass))
                            abort("folder", "file")
                    case _: ExternalResourceFolder =>
                        if (!classOf[ExternalResourceFolder].isAssignableFrom(rClass))
                            abort("file", "folder")
                }

                res.attach(factory)
            }

    }

    override def scan(scanningAction: String => Unit): Unit = {
        fsa.list(getAdapter)
            .map(_.getName)
            .filterNot(maintainer.isKnown)
            .foreach(scanningAction)

    }

    override def isPresentOnDrive(name: String): Boolean = fsa.exists(getLocation + "/" + name)
}

object DefaultExternalResourceFolder {
    def apply(fsa: FileSystemAdapter,
              path: String,
              listener: ResourceListener,
              parent: ExternalResourceFolder,
              options: Seq[AutomaticBehaviorOption] = Seq.empty): DefaultExternalResourceFolder = {
        val resources = new DefaultExternalResourceFolder(fsa, path, listener, parent)
        resources.getMaintainer.setBehaviors(options)
        resources
    }
}
