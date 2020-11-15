package fr.overridescala.vps.ftp.api.`extension`.packet

import fr.overridescala.vps.ftp.api.packet.Packet

trait PacketFactory[T <: Packet] {

    def decompose(implicit packet: T): Array[Byte]

    def canTransform(implicit bytes: Array[Byte]): Boolean

    def build(channelID: Int, senderID: String, targetID: String)(implicit bytes: Array[Byte]): T


}
