package fr.overridescala.vps.ftp.api.packet

import java.net.InetSocketAddress

trait PacketChannel {

    def sendPacket(header: String, content: Array[Byte]): Unit

    def sendPacket(header: String, content: String): Unit =
        sendPacket(header, content.getBytes)

    def sendPacket(header: String): Unit =
        sendPacket(header, "")

    def nextPacket(): DataPacket

    val ownerAddress: InetSocketAddress

}
