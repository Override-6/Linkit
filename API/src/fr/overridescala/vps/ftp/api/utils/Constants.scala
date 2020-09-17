package fr.overridescala.vps.ftp.api.utils

import java.net.InetSocketAddress

object Constants {

    val PORT = 4848
    val MAX_PACKET_LENGTH: Int = 512000
    val LOCALHOST: InetSocketAddress = new InetSocketAddress("localhost", PORT)
    val PUBLIC_ADDRESS: InetSocketAddress = Utils.getPublicAddress

}
