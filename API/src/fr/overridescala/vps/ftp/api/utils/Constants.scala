package fr.overridescala.vps.ftp.api.utils

import java.net.InetSocketAddress

object Constants {

    val PORT = 48484
    val MAX_PACKET_LENGTH: Int = 64000 // 64kb
    val LOCALHOST: InetSocketAddress = new InetSocketAddress("localhost", PORT)
    lazy val PUBLIC_ADDRESS: InetSocketAddress = Utils.getPublicAddress
    /**
     * The server identifier is forced to be this id.
     *  @see [[fr.overridescala.vps.ftp.api.Relay]]
     * */
    val SERVER_ID = "server"

}
