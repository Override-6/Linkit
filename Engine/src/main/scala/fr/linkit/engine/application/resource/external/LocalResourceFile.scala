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

package fr.linkit.engine.application.resource.external

import fr.linkit.api.application.resource.external._
import fr.linkit.api.application.resource.{ResourceListener, ResourcesMaintainer}
import fr.linkit.engine.application.resource.base.AbstractResource
import org.jetbrains.annotations.NotNull

import java.nio.file.{FileSystemException, Files, Path}
import java.util.zip.Adler32

class LocalResourceFile(@NotNull parent: ResourceFolder, path: Path) extends AbstractResource(parent, path) with ResourceFile with LocalResource {

    protected val entry = new DefaultResourceEntry[ResourceFile](this)

    override def getEntry: ResourceEntry[ResourceFile] = entry

    override def getChecksum: Long = try {
        val crc32 = new Adler32()
        val in    = Files.newInputStream(path)
        crc32.update(in.readAllBytes())
        in.close()
        crc32.getValue
    } catch {
        case _: FileSystemException => 0L
    }

    override protected def getMaintainer: ResourcesMaintainer = parent.getMaintainer

    override def createOnDisk(): Unit = Files.createFile(getPath)

    override def close(): Unit = entry.close()
}

object LocalResourceFile extends ResourceFactory[LocalResourceFile] {

    override def apply(adapter: Path, listener: ResourceListener, parent: ResourceFolder): LocalResourceFile = {
        apply(parent, adapter)
    }

    def apply(parent: ResourceFolder, adapter: Path): LocalResourceFile = new LocalResourceFile(parent, adapter)
}
