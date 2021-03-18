package fr.`override`.linkit.core.local.system.fsa.nio

import java.io.{InputStream, OutputStream}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import fr.`override`.linkit.api.local.system.fsa.FileAdapter

class NIOFileSystemAdapter private[fsa]() extends AbstractFileSystemAdapter {

    override val name: String = "java.nio.file"

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

    override def createAdapter(path: String): FileAdapter = new NIOFileAdapter(Paths.get(path), this)

    override def move(from: FileAdapter, to: FileAdapter): Unit = Files.move(from, to, StandardCopyOption.ATOMIC_MOVE)

    private implicit def toPath(fa: FileAdapter): Path = Paths.get(fa.getPath)
}
