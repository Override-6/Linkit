/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.core.local.system.fsa.nio

import fr.linkit.api.local.system.fsa.FileAdapter

import java.io.{InputStream, OutputStream}
import java.net.URI
import java.nio.file.{Files, OpenOption, Path, StandardOpenOption}


case class NIOFileAdapter private[nio](path: Path, fsa: NIOFileSystemAdapter) extends FileAdapter {

    override def getPath: String = path.toString

    override def getAbsolutePath: String = path.toAbsolutePath.toString

    override def getSize: Long = Files.size(path)

    override def getParent(level: Int): FileAdapter = {
        var parent = path
        for (_ <- 0 to level) {
            parent = parent.getParent
        }
        fsa.getAdapter(parent.toString)
    }

    override def toUri: URI = path.toUri

    override def resolveSibling(path: String): FileAdapter = resolveSiblings(fsa.getAdapter(path))

    override def resolveSiblings(fa: FileAdapter): FileAdapter = {
        val resolved = path.resolveSibling(path.getParent)
        fsa.getAdapter(resolved.toString)
    }

    override def isDirectory: Boolean = Files.isDirectory(path)

    override def isReadable: Boolean = Files.isReadable(path)

    override def isWritable: Boolean = Files.isWritable(path)

    override def delete(): Boolean = Files.deleteIfExists(path)

    override def exists: Boolean = Files.exists(path)

    override def notExists: Boolean = Files.notExists(path)

    override def newInputStream(append: Boolean = false): InputStream = {
        Files.newInputStream(path, options(append): _*)
    }

    override def newOutputStream(append: Boolean = false): OutputStream = {
        Files.newOutputStream(path, options(append): _*)
    }

    override def write(bytes: Array[Byte], append: Boolean = false): Unit = {
        Files.write(path, bytes, options(append): _*)
    }

    private def options(append: Boolean): Array[OpenOption] =
        if (append) Array(StandardOpenOption.APPEND) else Array[OpenOption]()
}
