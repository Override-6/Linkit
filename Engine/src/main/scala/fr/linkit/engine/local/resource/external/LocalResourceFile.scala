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

package fr.linkit.engine.local.resource.external

import fr.linkit.api.local.resource.external._
import fr.linkit.api.local.resource.{ResourceListener, ResourcesMaintainer}
import fr.linkit.engine.local.resource.base.AbstractResource
import org.jetbrains.annotations.NotNull

import java.nio.file.{FileSystemException, Files, Path}
import java.util.zip.Adler32

class LocalResourceFile(@NotNull parent: ResourceFolder, path: Path) extends AbstractResource(parent, path) with ResourceFile with LocalResource {

    protected val entry = new DefaultResourceEntry[ResourceFile](this)

    override def getEntry: ResourceEntry[ResourceFile] = entry

    override def getChecksum: Long = try {
        val crc32 = new Adler32()
        crc32.update(Files.readAllBytes(path))
        crc32.getValue
    } catch {
        case _: FileSystemException => 0L
    }

    override protected def getMaintainer: ResourcesMaintainer = parent.getMaintainer

    override def createOnDisk(): Unit = {
        Files.createDirectories(path.getParent)
        Files.createFile(path)
    }

    override def close(): Unit = entry.close()
}

object LocalResourceFile extends ResourceFactory[LocalResourceFile] {

    override def apply(path: Path, listener: ResourceListener, parent: ResourceFolder): LocalResourceFile = {
        apply(parent, path)
    }

    def apply(parent: ResourceFolder, path: Path): LocalResourceFile = new LocalResourceFile(parent, path)
}
