package fr.`override`.linkit.core.local.system.fsa.io

import java.io.{File, InputStream, OutputStream}

import fr.`override`.linkit.api.local.system.fsa.FileAdapter

class IOFileSystemAdapter private[fsa]() extends AbstractFileSystemAdapter {
    override val name: String = "java.io"

    override def createAdapter(path: String): FileAdapter = new IOFileAdapter(path, this)

    override def createDirectories(path: FileAdapter): Unit = path.getPath.mkdirs()

    override def create(path: FileAdapter): Unit = path.getPath.createNewFile()

    override def list(path: FileAdapter): Array[FileAdapter] = {
        val files = path.getPath.listFiles()
        if (files == null)
            return Array()
        files.map(getAdapter)
    }

    override def newInputStream(path: FileAdapter): InputStream = path.newInputStream()

    override def newOutputStream(path: FileAdapter): OutputStream = path.newOutputStream()

    override def delete(path: FileAdapter): Unit = path.delete()

    private implicit def toFile(path: String): File = new File(path)

    private def getAdapter(file: File): FileAdapter = super.getAdapter(file.toString)

    override def move(from: FileAdapter, to: FileAdapter): Unit = {
        val bytes = readAllBytes(from)
        if (to notExists) {
            createDirectories(to)
            delete(to)
            create(to)
        }

        val output = to.newOutputStream()
        try output.write(bytes)
        finally {
            output.flush()
            output.close()
            from.delete()
        }

    }
}
