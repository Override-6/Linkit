package fr.overridescala.vps.ftp.api.packet

trait PacketChannelManager {

    def addPacket(packet: DataPacket): Boolean

}