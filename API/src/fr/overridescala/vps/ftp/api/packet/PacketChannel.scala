package fr.overridescala.vps.ftp.api.packet

import java.net.InetSocketAddress

trait PacketChannel {

    def sendPacket(packet: TaskPacket): Unit

    def nextPacket(): TaskPacket

    val getOwnerAddress: InetSocketAddress

}
