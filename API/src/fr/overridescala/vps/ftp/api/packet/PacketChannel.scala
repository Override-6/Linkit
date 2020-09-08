package fr.overridescala.vps.ftp.api.packet

import java.net.InetSocketAddress

trait PacketChannel {

    def sendPacket(header: String, content: Array[Byte] = Array()): Unit

    def nextPacket(): DataPacket

    val ownerAddress: InetSocketAddress

}
