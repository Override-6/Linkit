package fr.overridescala.vps.ftp.api.transfer

import java.net.InetSocketAddress
import java.nio.file.{Files, Path}

import fr.overridescala.vps.ftp.api.utils.{Constants, Utils}

/**
 * Description of an known or unknown file / folder hosted by other Relays on the network.
 *
 * @param path the file / folder path
 * @param ownerAddress the owner of this presumed file / folder
 * @param isDirectory if the path points to a Folder
 * @param rootPath the directory where this file is set. Or the same path if this path points to a Folder
 * @param size the size of the file / folder
 * */
case class FileDescription private(path: String,
                                   ownerAddress: InetSocketAddress,
                                   isDirectory: Boolean,
                                   rootPath: String,
                                   size: Long) extends Serializable

object FileDescription extends Serializable {

    val serialVersionUID = 151

    /**
     * @return a Description of a local path
     * */
    def fromLocal(stringPath: String): FileDescription = {
        val path = Utils.formatPath(stringPath).toRealPath()
        builder()
                .setOwner(Constants.PUBLIC_ADDRESS)
                .setPath(path.toString)
                .setSize(getSize(path))
                .build()
    }

    private def getSize(path: Path): Long = {
        if (Files.isDirectory(path)) {
            return Files.list(path)
                    .mapToLong(getSize)
                    .sum()
        }
        Files.size(path)
    }

    /**
     * @return this FileDescription builder
     * */
    def builder(): Builder = new Builder()


    class Builder {
        private var path: String = _
        private var ownerAddress: InetSocketAddress = _
        private var size: Long = _
        private var isDirectory: Boolean = false
        private var rootPath: Path = _


        def setPath(path: String): Builder = {
            this.path = path
            val filePath = Path.of(path)
            rootPath = if (Files.isDirectory(filePath)) filePath else filePath.getParent
            isDirectory = filePath.toFile.getName.contains(".")
            this
        }

        def setOwner(address: InetSocketAddress): Builder = {
            this.ownerAddress = address
            this
        }

        def setSize(size: Long): Builder = {
            this.size = size
            this
        }

        def build(): FileDescription =
            new FileDescription(path, ownerAddress, isDirectory, rootPath.toString, size)

    }

}
