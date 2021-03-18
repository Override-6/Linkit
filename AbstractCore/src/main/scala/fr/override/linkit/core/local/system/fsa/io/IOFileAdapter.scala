package fr.`override`.linkit.core.local.system.fsa.io

import java.io.{File, FileInputStream, FileOutputStream, InputStream, OutputStream}
import java.net.URI

import fr.`override`.linkit.api.local.system.fsa.FileAdapter

class IOFileAdapter private[io](file: File, fsa: IOFileSystemAdapter) extends FileAdapter {

    override def getPath: String = file.getPath

    override def getAbsolutePath: String = file.getAbsolutePath

    override def getSize: Long = file.length()

    override def getParent(level: Int): FileAdapter = {
        var parent = file
        for (_ <- 0 to level)
            parent = parent.getParentFile
        fsa.getAdapter(parent.toString)
    }

    override def toUri: URI = file.toURI

    override def resolveSibling(path: String): FileAdapter = fsa.getAdapter(getPath + File.separatorChar + path)

    override def resolveSiblings(path: FileAdapter): FileAdapter = resolveSibling(path.getPath)

    override def isDirectory: Boolean = file.isDirectory

    override def isReadable: Boolean = file.canRead

    override def isWritable: Boolean = file.canWrite

    override def delete(): Boolean = file.delete()

    override def exists: Boolean = file.exists()

    override def notExists: Boolean = !exists

    override def newInputStream(append: Boolean = false): InputStream = new FileInputStream(file)

    override def newOutputStream(append: Boolean = false): OutputStream = new FileOutputStream(file)

    override def write(bytes: Array[Byte], append: Boolean = false): Unit = {
        val out = newOutputStream(append)
        try out.write(bytes)
        finally {
            out.flush()
            out.close()
        }
    }


}
