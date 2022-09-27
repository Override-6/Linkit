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

package fr.linkit.engine.application.resource

import fr.linkit.api.application.resource.ResourcesMaintainer
import fr.linkit.api.application.resource.local.{Resource, ResourceFolder}
import fr.linkit.api.internal.system.Versions

import java.nio.file.Path

abstract class AbstractResource(parent: Option[ResourceFolder], path: Path) extends Resource {

    override     val name: String = path.getFileName.toString
    private lazy val lastModified = getMaintainer.getLastModified(name)

    protected def getMaintainer: ResourcesMaintainer

    override def getLocation: String = {
        parent.map(_.getLocation + "/" + name).getOrElse("/")
    }

    override def getLastModified: Versions = lastModified

    override def getParent: Option[ResourceFolder] = parent

    override def getRoot: ResourceFolder = {
        var lastParent: ResourceFolder = parent.orNull
        while (lastParent != null)
            lastParent = lastParent.getParent.orNull
        lastParent
    }

    override def getPath: Path = path

    override def getChecksum: Long

    override def getLastChecksum: Long = getMaintainer.getLastChecksum(name)

}
