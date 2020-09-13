package fr.overridescala.vps.ftp.api.transfer

import java.net.InetSocketAddress
import java.nio.file.{Files, NoSuchFileException, Path}

import fr.overridescala.vps.ftp.api.utils.Constants

case class TransferableFile private(path: String,
                                    ownerAddress: InetSocketAddress,
                                    isDirectory: Boolean,
                                    rootPath: String,
                                    size: Long) extends Serializable {


}

object TransferableFile {

    def fromLocal(stringPath: String): TransferableFile = {
        val path: Path = Path.of(stringPath.replace("/", "\\"))
        if (Files notExists path) {
            throw new NoSuchFileException(stringPath)
        }
        val isDirectory = Files.isDirectory(path)
        val rootPath = if (Files.isDirectory(path)) path else path.getParent
        new TransferableFile(stringPath, Constants.PUBLIC_ADDRESS, isDirectory, rootPath.toString, Files.size(path))

    }

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

        def build(): TransferableFile =
            new TransferableFile(path, ownerAddress, isDirectory, rootPath.toString, size)

    }

}
