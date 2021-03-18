package fr.`override`.linkit.core.local.system.fsa.nio

import java.io.{InputStream, OutputStream}
import java.net.URI
import java.nio.file.{Files, OpenOption, Path, StandardOpenOption}

import fr.`override`.linkit.api.local.system.fsa.FileAdapter


class NIOFileAdapter private[nio](path: Path, fsa: NIOFileSystemAdapter) extends FileAdapter {

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
