/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.system.fsa.nio

import fr.linkit.api.internal.system.fsa.FileAdapter
import fr.linkit.engine.internal.system.fsa.AbstractFileSystemAdapter

import java.io.{File, InputStream, OutputStream}
import java.net.URI
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

class NIOFileSystemAdapter private[fsa]() extends AbstractFileSystemAdapter {

    protected def this(other: NIOFileSystemAdapter) = {
        this()
    }

    override def createDirectories(path: FileAdapter): Unit = Files.createDirectories(path)

    override def create(path: FileAdapter): Unit = Files.createFile(path)

    override def list(path: FileAdapter): Array[FileAdapter] = {
        Files
                .list(path)
                .map(getAdapter)
                .toArray[FileAdapter](l => new Array[FileAdapter](l))
    }

    override def newInputStream(path: FileAdapter): InputStream = Files.newInputStream(path)

    override def newOutputStream(path: FileAdapter): OutputStream = Files.newOutputStream(path)

    override def delete(path: FileAdapter): Unit = Files.deleteIfExists(path)

    private def getAdapter(path: Path): FileAdapter = super.getAdapter(path.toString)

    override def createAdapter(path: String): FileAdapter = {
        NIOFileAdapter(Paths.get(path
                .replace("\\", File.separator)
                .replace("/", File.separator)
        ), this)
    }

    override def createAdapter(uri: URI): FileAdapter = NIOFileAdapter(Paths.get(uri), this)

    override def move(from: FileAdapter, to: FileAdapter): Unit = Files.move(from, to, StandardCopyOption.ATOMIC_MOVE)

    private implicit def toPath(fa: FileAdapter): Path = Paths.get(fa.getPath)
}
