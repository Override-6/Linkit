package fr.`override`.linkit.skull.internal.system.fsa

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
