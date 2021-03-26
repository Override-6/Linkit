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

package fr.linkit.api.local.system.fsa

import java.io.{InputStream, OutputStream}
import java.net.URI

//TODO implements more methods from java.nio.file.Path
trait FileAdapter {

    override def toString: String = getAbsolutePath

    override def equals(obj: Any): Boolean = obj != null && obj.getClass == getClass && obj.toString == toString

    def getPath: String

    def getAbsolutePath: String

    def getSize: Long

    def getParent: FileAdapter = getParent(1)

    def getParent(level: Int): FileAdapter

    def resolveSibling(path: String): FileAdapter

    def resolveSiblings(path: FileAdapter): FileAdapter

    def toUri: URI

    def isDirectory: Boolean

    def isReadable: Boolean

    def isWritable: Boolean

    def delete(): Boolean

    def exists: Boolean

    def notExists: Boolean

    def newInputStream(append: Boolean = false): InputStream

    def newOutputStream(append: Boolean = false): OutputStream

    def write(bytes: Array[Byte], append: Boolean = false): Unit



}
