package fr.overridescala.vps.ftp.api.packet

trait PacketChannel {

    def sendPacket(packet: TaskPacket): Unit

    def nextPacket(): TaskPacket

}
