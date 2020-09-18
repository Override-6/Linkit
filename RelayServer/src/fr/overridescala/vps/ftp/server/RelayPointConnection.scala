package fr.overridescala.vps.ftp.server

import java.net.SocketAddress

import fr.overridescala.vps.ftp.api.packet.SimplePacketChannel

case class RelayPointConnection(var identifier: String,
                                packetChannel: SimplePacketChannel,
                                address: SocketAddress) {

}
