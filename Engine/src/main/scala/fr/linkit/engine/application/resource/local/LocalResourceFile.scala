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

package fr.linkit.engine.application.resource.local

import fr.linkit.api.application.resource.local._
import fr.linkit.api.application.resource.representation.{ResourceRepresentation, ResourceRepresentationFactory}
import fr.linkit.api.application.resource.{ResourceListener, ResourcesMaintainer}
import fr.linkit.engine.application.resource.AbstractResource
import org.jetbrains.annotations.NotNull

import java.nio.file.{FileSystemException, Files, Path}
import java.util.zip.Adler32
import scala.reflect.ClassTag

class LocalResourceFile(parent: Option[ResourceFolder], path: Path) extends AbstractResource(parent, path) with LocalFile {

    if (parent.isEmpty)
        throw new IllegalStateException("Resource file has no parent.")
    
    protected val entry = new DefaultResourceEntry[ResourceFile](this)

    override def getEntry: ResourceEntry[ResourceFile] = entry

    override def getChecksum: Long = try {
        //TODO Better checksum technique (in.readAllBytes may cause OutOfMemory issues for very big files)
        val crc32 = new Adler32()
        val in    = Files.newInputStream(path)
        crc32.update(in.readAllBytes())
        in.close()
        crc32.getValue
    } catch {
        case _: FileSystemException => 0L
    }

    override protected def getMaintainer: ResourcesMaintainer = parent.get.getMaintainer

    override def createOnDisk(): Unit = Files.createFile(getPath)

    override def close(): Unit = entry.close()
}

object LocalResourceFile extends ResourceFactory[LocalFile] {

    override def apply(adapter: Path, listener: ResourceListener, parent: Option[ResourceFolder]): LocalResourceFile = {
        apply(parent, adapter)
    }

    def apply(parent: Option[ResourceFolder], adapter: Path): LocalResourceFile = new LocalResourceFile(parent, adapter)
    
}
