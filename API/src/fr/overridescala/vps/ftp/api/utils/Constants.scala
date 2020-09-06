package fr.overridescala.vps.ftp.api.utils

import java.net.InetSocketAddress

object Constants {

    val PORT = 4848
    val MAX_PACKET_LENGTH: Int = 4096 * 8 * 8
    val PUBLIC_ADDRESS: InetSocketAddress = new InetSocketAddress("localhost", PORT)
    //val PUBLIC_ADDRESS: InetSocketAddress = Utils.getPublicAddress

}
