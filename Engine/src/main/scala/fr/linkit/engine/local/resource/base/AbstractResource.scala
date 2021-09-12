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

package fr.linkit.engine.local.resource.base

import fr.linkit.api.local.resource.ResourcesMaintainer
import fr.linkit.api.local.resource.external.{Resource, ResourceFolder}
import fr.linkit.api.local.system.Versions
import fr.linkit.engine.local.system.{DynamicVersions, StaticVersions}
import org.jetbrains.annotations.Nullable

import java.nio.file.Path

abstract class AbstractResource(@Nullable parent: ResourceFolder, path: Path) extends Resource {

    override     val name: String = path.getFileName.toString
    private lazy val lastModified = getMaintainer.getLastModified(name)

    protected def getMaintainer: ResourcesMaintainer

    override def getLocation: String = {
        if (parent == null)
            return "/"
        parent.getLocation + '/' + name
    }

    override def getLastModified: Versions = lastModified

    override def getParent: ResourceFolder = parent

    override def getRoot: ResourceFolder = {
        var lastParent: ResourceFolder = getParent
        while (lastParent != null)
            lastParent = lastParent.getParent
        lastParent
    }

    override def getPath: Path = path

    override def getChecksum: Long

    override def getLastChecksum: Long = getMaintainer.getLastChecksum(name)

    override def markAsModifiedByCurrentApp(): Unit = {
        lastModified match {
            case dynamic: DynamicVersions => dynamic.setAll(StaticVersions.currentVersions)
            case _                        =>
            //If the Versions implementation isn't dynamic, this means that we may not
            //Update the versions.
        }
        if (getParent != null)
            getParent.markAsModifiedByCurrentApp()
    }

}
