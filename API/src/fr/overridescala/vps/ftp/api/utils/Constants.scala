package fr.overridescala.vps.ftp.api.utils

import java.net.InetSocketAddress

object Constants {

    val PORT = 4849
    val MAX_PACKET_LENGTH: Int = 4096
    val PUBLIC_ADDRESS: InetSocketAddress = new InetSocketAddress("localhost", PORT)
    //  val PUBLIC_ADDRESS: InetSocketAddress = Utils.getPublicAddress

}
