package fr.overridescala.vps.ftp.api.packet.fundamental

import fr.overridescala.vps.ftp.api.`extension`.packet.PacketFactory
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}

case class ErrorPacket (override val channelID: Int,
                       override val senderID: String,
                       override val targetID: String,
                       errorType: String,
                       errorMsg: String,
                       cause: String = "") extends Packet {

    def printError(): Unit = {
        Console.err.println(s"$errorType: $errorMsg")
        if (!cause.isEmpty)
            Console.err.print(s"caused by: $cause ")
        Console.err.println(s"from relay $senderID")
    }

}


object ErrorPacket {

    def apply(errorType: String, msg: String, cause: String)(implicit channel: PacketChannel): ErrorPacket =
        ErrorPacket(channel.channelID, channel.ownerID, channel.connectedID, errorType, msg, cause)

    def apply(errorType: String, msg: String)(implicit channel: PacketChannel): ErrorPacket =
        apply(errorType, msg, "")

    object Factory extends PacketFactory[ErrorPacket] {

        import fr.overridescala.vps.ftp.api.`extension`.packet.PacketUtils._

        private val TYPE = "[err]".getBytes
        private val MSG = "<msg>".getBytes
        private val CAUSE = "<cause>".getBytes

        override def decompose(implicit packet: ErrorPacket): Array[Byte] = {
            val errorType = packet.errorType.getBytes
            val errorMsg = packet.errorMsg.getBytes
            val cause = packet.cause.getBytes
            TYPE ++ errorType ++
                    MSG ++ errorMsg ++
                    CAUSE ++ cause
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean =
            bytes.startsWith(TYPE)

        override def build(channelID: Int, sender: String, target: String)(implicit bytes: Array[Byte]): ErrorPacket = {
            val errorType = cutString(TYPE, MSG)
            val msg = cutString(MSG, CAUSE)
            val cause = new String(cutEnd(CAUSE))
            ErrorPacket(channelID, sender, target, errorType, msg, cause)
        }

    }

}
