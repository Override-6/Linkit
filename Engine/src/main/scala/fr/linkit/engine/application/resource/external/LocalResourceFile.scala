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

package fr.linkit.engine.application.resource.external

import fr.linkit.api.application.resource.external._
import fr.linkit.api.application.resource.{ResourceListener, ResourcesMaintainer}
import fr.linkit.api.internal.system.fsa.FileAdapter
import fr.linkit.engine.application.resource.base.AbstractResource
import org.jetbrains.annotations.NotNull

import java.nio.file.FileSystemException
import java.util.zip.Adler32

class LocalResourceFile(@NotNull parent: ResourceFolder, adapter: FileAdapter) extends AbstractResource(parent, adapter) with ResourceFile with LocalResource {

    protected val entry = new DefaultResourceEntry[ResourceFile](this)

    override def getEntry: ResourceEntry[ResourceFile] = entry

    override def getChecksum: Long = try {
        val crc32 = new Adler32()
        val in    = adapter.newInputStream()
        crc32.update(in.readAllBytes())
        in.close()
        crc32.getValue
    } catch {
        case _: FileSystemException => 0L
    }

    override protected def getMaintainer: ResourcesMaintainer = parent.getMaintainer

    override def createOnDisk(): Unit = getAdapter.createAsFile()

    override def close(): Unit = entry.close()
}

object LocalResourceFile extends ResourceFactory[LocalResourceFile] {

    override def apply(adapter: FileAdapter, listener: ResourceListener, parent: ResourceFolder): LocalResourceFile = {
        apply(parent, adapter)
    }

    def apply(parent: ResourceFolder, adapter: FileAdapter): LocalResourceFile = new LocalResourceFile(parent, adapter)
}
