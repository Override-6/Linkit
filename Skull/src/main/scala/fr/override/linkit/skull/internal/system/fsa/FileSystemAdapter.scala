package fr.`override`.linkit.skull.internal.system.fsa

import java.io.{IOException, InputStream, OutputStream}

/**
 * <p>
 *     It is possible to think that a FileSystem adapter should be overkill, that it's way better
 *     to use the java.nio.file system. And this is a good critic. The problem is, for some
 *     Java jre installations, nio.file is removed (ex: for android SDK < 26). To resolve this problem,
 *     an adapter was released. The utilisation of the adapter system is not inescapable,
 *     but is deeply recommended if you want to release an extension that could run on every java platforms</p>
 * <p>
 *     The adapters design was inspired from the java.nio.file.Paths (for [[FileAdapter]]) & java.nio.file.{Files, Paths} (for [[FileSystemAdapter]])
 *     So the user should notice a lot of similarity with the nio library.
 * */
//TODO implements more methods from java.nio.file.Files
trait FileSystemAdapter {

    val name: String

    implicit def getAdapter(path: String): FileAdapter

    def createDirectories(path: String): Unit = createDirectories(getAdapter(path))

    def createDirectories(fa: FileAdapter): Unit

    def create(path: String): Unit = create(getAdapter(path))

    def create(fa: FileAdapter): Unit

    @throws[IOException]("If the file is not a folder")
    def list(path: String): Array[FileAdapter] = list(getAdapter(path))

    def list(fa: FileAdapter): Array[FileAdapter]

    def newInputStream(path: String): InputStream = newInputStream(getAdapter(path))

    def newInputStream(fa: FileAdapter): InputStream

    def newOutputStream(path: String): OutputStream = newOutputStream(getAdapter(path))

    def newOutputStream(fa: FileAdapter): OutputStream

    def readAllBytes(path: String): Array[Byte] = readAllBytes(getAdapter(path))

    def readAllBytes(fa: FileAdapter): Array[Byte] = {
        val input = fa.newInputStream()
        try {
            val size = input.available()
            val buff = new Array[Byte](size)
            input.read(new Array[Byte](size))

            buff
        } finally {
            input.close()
        }
    }

    def deleteIfExists(path: String): Unit = delete(getAdapter(path))

    def delete(fa: FileAdapter): Unit

    def move(from: FileAdapter, to: FileAdapter): Unit

    def moveTo(from: String, to: String): Unit = move(getAdapter(from), getAdapter(to))

}
