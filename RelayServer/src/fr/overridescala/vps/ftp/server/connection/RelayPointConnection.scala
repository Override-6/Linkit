package fr.overridescala.vps.ftp.server.connection

import java.net.SocketAddress

import fr.overridescala.vps.ftp.api.packet.SimplePacketChannel

case class RelayPointConnection protected(packetChannel: SimplePacketChannel) {

    val identifier: String = packetChannel.ownerID
    val address: SocketAddress = packetChannel.ownerAddress

}
