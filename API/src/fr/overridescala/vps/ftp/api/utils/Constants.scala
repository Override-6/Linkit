package fr.overridescala.vps.ftp.api.utils

import java.net.InetSocketAddress

object Constants {

    val PORT = 48484
    val MAX_PACKET_LENGTH: Int = 4096 * 8
    val LOCALHOST: InetSocketAddress = new InetSocketAddress("localhost", PORT)
    val PUBLIC_ADDRESS: InetSocketAddress = Utils.getPublicAddress
    val SELF_ID: String = "%self%"
    val SERVER_ID = "%server%"

}
