package fr.overridescala.vps.ftp.`extension`.ppc.logic

import fr.overridescala.vps.ftp.api.packet.ext.PacketFactory
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}

case class MovePacket private(override val senderID: String,
                              override val targetID: String,
                              override val channelID: Int,
                              move: MoveType) extends Packet {


}

object MovePacket {
    def apply(move: MoveType)(implicit channel: PacketChannel): MovePacket =
        new MovePacket(
            channel.ownerIdentifier,
            channel.connectedIdentifier,
            channel.channelID,
            move
        )

    object Factory extends PacketFactory[MovePacket] {

        private val TYPE_FLAG = "[RPS]".getBytes
        private val SENDER_FLAG = "<sender>".getBytes
        private val TARGET_FLAG = "<target>".getBytes
        private val MOVE_FLAG = "<move>".getBytes

        override def decompose(implicit packet: MovePacket): Array[Byte] = {
            val channelID = packet.channelID.toString.getBytes
            val sender = packet.senderID.getBytes
            val target = packet.targetID.getBytes
            val move = packet.move.name().getBytes
            TYPE_FLAG ++ channelID ++
                    SENDER_FLAG ++ sender ++
                    TARGET_FLAG ++ target ++
                    MOVE_FLAG ++ move
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean = bytes.startsWith(TYPE_FLAG)

        override def build(implicit bytes: Array[Byte]): MovePacket = {
            val channelID = cutString(TYPE_FLAG, SENDER_FLAG).toInt
            val sender = cutString(SENDER_FLAG, TARGET_FLAG)
            val target = cutString(TARGET_FLAG, MOVE_FLAG)
            val moveName = new String(cutEnd(MOVE_FLAG))
            new MovePacket(sender, target, channelID, MoveType.valueOf(moveName))
        }
    }
}
