package fr.overridescala.vps.ftp.api.utils

import java.net.{InetSocketAddress, URI}
import java.nio.file.{Path, Paths}

object Constants {

    val PORT = 48484
    val JAR_LOCATION: Path = Paths.get(getClass.getProtectionDomain.getCodeSource.getLocation.toURI)
    val FOLDER_JAR_LOCATION: Path = JAR_LOCATION.getParent.toRealPath()
    lazy val PUBLIC_ADDRESS: InetSocketAddress = Utils.getPublicAddress
    /**
     * The server identifier is forced to be this id.
     *  @see [[fr.overridescala.vps.ftp.api.Relay]]
     * */
    val SERVER_ID = "server"

}
