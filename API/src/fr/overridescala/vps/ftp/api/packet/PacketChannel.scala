package fr.overridescala.vps.ftp.api.packet

trait PacketChannel {

    def sendPacket(header: String, content: Array[Byte]): Unit

    def sendPacket(header: String, content: String): Unit =
        sendPacket(header, content.getBytes)

    def sendPacket(header: String): Unit =
        sendPacket(header, "")

    def nextPacket(): DataPacket

    def haveMorePackets: Boolean

    val ownerID: String


}
