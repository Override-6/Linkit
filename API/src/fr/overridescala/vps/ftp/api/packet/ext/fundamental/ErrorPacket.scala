package fr.overridescala.vps.ftp.api.packet.ext.fundamental

import fr.overridescala.vps.ftp.api.packet.ext.PacketFactory
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}

case class ErrorPacket(override val channelID: Int,
                       override val senderID: String,
                       override val targetID: String,
                       errorType: String,
                       errorMsg: String,
                       cause: String = "") extends Packet {

    def printError(): Unit = {
        def println(x: Any): Unit = Console.err.println(x)

        println(s"$errorType: $errorMsg")
        if (!cause.isEmpty)
            println(s"caused by: $cause")
    }
}


object ErrorPacket {
    val ABORT_TASK: String = "ABORT_TASK"

    def apply(errorType: String, msg: String, cause: String)(implicit channel: PacketChannel): ErrorPacket =
        ErrorPacket(channel.channelID, channel.ownerIdentifier, channel.connectedIdentifier, errorType, msg, cause)

    def apply(errorType: String, msg: String)(implicit channel: PacketChannel): ErrorPacket =
        apply(errorType, msg, "")

    object Factory extends PacketFactory[ErrorPacket] {

        import fr.overridescala.vps.ftp.api.packet.ext.PacketUtils._

        private val TYPE = "[err]".getBytes
        private val ERROR_TYPE = "<type>".getBytes
        private val MSG = "<msg>".getBytes
        private val CAUSE = "<cause>".getBytes

        override def decompose(implicit packet: ErrorPacket): Array[Byte] = {
            val channelID = packet.channelID.toString.getBytes
            val errorType = packet.errorType.getBytes
            val errorMsg = packet.errorMsg.getBytes
            val cause = packet.cause.getBytes
            TYPE ++ channelID ++
                    ERROR_TYPE ++ errorType ++
                    MSG ++ errorMsg ++
                    CAUSE ++ cause
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean =
            bytes.containsSlice(TYPE)

        override def build(sender: String, target: String)(implicit bytes: Array[Byte]): ErrorPacket = {
            val channelID = cutString(TYPE, ERROR_TYPE).toInt
            val errorType = cutString(ERROR_TYPE, MSG)
            val msg = cutString(MSG, CAUSE)
            val cause = new String(cutEnd(CAUSE))
            ErrorPacket(channelID, sender, target, errorType, msg, cause)
        }

    }

}
