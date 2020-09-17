package fr.overridescala.vps.ftp.api.transfer

import java.net.InetSocketAddress
import java.nio.file.{Files, NoSuchFileException, Path}

import fr.overridescala.vps.ftp.api.utils.{Constants, Utils}

case class TransferableFile private(path: String,
                                    ownerAddress: InetSocketAddress,
                                    isDirectory: Boolean,
                                    rootPath: String,
                                    size: Long) extends Serializable {



}

object TransferableFile extends Serializable {

    val serialVersionUID = 151

    def fromLocal(stringPath: String): TransferableFile = {
        val path = Utils.formatPath(stringPath).toRealPath()
        builder()
                .setOwner(Constants.PUBLIC_ADDRESS)
                .setPath(path.toString)
                .setSize(Files.size(path))
                .build()
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
