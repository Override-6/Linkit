package fr.overridescala.vps.ftp.api.transfer

import java.net.InetSocketAddress

class FileDescriptionBuilder {
    var path: String = null
    var ownerAddress: InetSocketAddress = null
    var isDirectory: Boolean = _
    var rootPath: String = null
    var size: Long = _

    def build(): FileDescription = new FileDescription(path, ownerAddress, isDirectory, rootPath, size)
}
