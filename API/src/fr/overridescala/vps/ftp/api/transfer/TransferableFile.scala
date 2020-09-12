package fr.overridescala.vps.ftp.api.transfer

import java.net.InetSocketAddress
import java.nio.file.{Files, NoSuchFileException, Path}

import fr.overridescala.vps.ftp.api.utils.Constants

class TransferableFile private(private val path: String,
                               private val ownerAddress: InetSocketAddress,
                               private val size: Long) extends Serializable {


    def getPath: String = path

    def getOwnerAddress: InetSocketAddress = ownerAddress

    def getSize: Long = size

}

object TransferableFile {

    def fromLocal(stringPath: String): TransferableFile = {
        val path: Path = Path.of(stringPath.replace("/", "\\"))
        if (Files notExists path) {
            throw new NoSuchFileException(stringPath)
        }
        new TransferableFile(stringPath, Constants.PUBLIC_ADDRESS, Files size path)
    }

    def builder(): Builder = new Builder()


    class Builder {
        private var path: String = _
        private var ownerAddress: InetSocketAddress = _
        private var size: Long = _

        def setPath(path: String): Builder = {
            this.path = path
            this
        }

        def setOwner(address: InetSocketAddress) = {
            this.ownerAddress = address
            this
        }

        def setSize(size: Long): Builder = {
            this.size = size
            this
        }

        def build(): TransferableFile = new TransferableFile(path, ownerAddress, size)

    }

}
