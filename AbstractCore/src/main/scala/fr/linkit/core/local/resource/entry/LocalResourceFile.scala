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

import fr.linkit.api.local.resource.external.{ResourceEntry, ResourceFile}
import fr.linkit.api.local.system.fsa.FileAdapter
import fr.linkit.core.local.resource.ResourceFolderMaintainer
import org.jetbrains.annotations.NotNull

import java.util.zip.Adler32

class LocalResourceFile(@NotNull parent: LocalResourceFolder, adapter: FileAdapter) extends AbstractResource(parent, adapter) with ResourceFile {

    println(s"Created resource File $getLocation")


    override def getChecksum: Long = {
        val crc32 = new Adler32()
        val in    = adapter.newInputStream()
        crc32.update(in.readAllBytes())
        in.close()
        crc32.getValue
    }

    override protected def getMaintainer: ResourceFolderMaintainer = parent.getMaintainer
}
