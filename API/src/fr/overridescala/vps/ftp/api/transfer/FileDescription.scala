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
case class FileDescription(path: String,
                      ownerAddress: InetSocketAddress,
                      isDirectory: Boolean,
                      rootPath: String,
                      size: Long) extends Serializable {

}

object FileDescription extends Serializable {

    val serialVersionUID = 151

    /**
     * @return a Description of a local path
     * */
    def fromLocal(stringPath: String): FileDescription = {
        val realPath = Utils.formatPath(stringPath).toRealPath()
        val dirRootPath = if (Files.isDirectory(realPath)) realPath else realPath.getParent

        new FileDescriptionBuilder() {
            size = getSize(realPath)
            path = realPath.toString
            rootPath = dirRootPath.toString
            isDirectory = Files.isDirectory(realPath)
            ownerAddress = Constants.PUBLIC_ADDRESS
        }.build()
    }

    private def getSize(path: Path): Long = {
        if (Files.isDirectory(path)) {
            return Files.list(path)
                    .mapToLong(getSize)
                    .sum()
        }
        Files.size(path)
    }

}
