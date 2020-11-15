package fr.overridescala.vps.ftp.`extension`.ppc.logic

import fr.overridescala.vps.ftp.api.`extension`.packet.PacketFactory
import fr.overridescala.vps.ftp.api.`extension`.packet.PacketUtils.{cutEnd, cutString}
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}

case class MovePacket private(override val senderID: String,
                              override val targetID: String,
                              override val channelID: Int,
                              move: MoveType) extends Packet {


}

object MovePacket {
    def apply(move: MoveType)(implicit channel: PacketChannel): MovePacket =
        new MovePacket(
            channel.ownerID,
            channel.connectedID,
            channel.channelID,
            move
        )

    object Factory extends PacketFactory[MovePacket] {

        private val TYPE_FLAG = "[RPS]".getBytes

        override def decompose(implicit packet: MovePacket): Array[Byte] = {
            val move = packet.move.name().getBytes
            TYPE_FLAG ++ move
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean = bytes.startsWith(TYPE_FLAG)

        override def build(channelID: Int, senderID: String, targetID: String)(implicit bytes: Array[Byte]): MovePacket = {
            val moveName = new String(cutEnd(TYPE_FLAG))
            new MovePacket(senderID, targetID, channelID, MoveType.valueOf(moveName))
        }
    }

}
