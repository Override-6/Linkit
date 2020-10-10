package fr.overridescala.vps.ftp.api.packet.ext.fundamental

import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}
import fr.overridescala.vps.ftp.api.packet.ext.PacketFactory

case class ErrorPacket(override val channelID: Int,
                       override val senderIdentifier: String,
                       override val targetIdentifier: String,
                       errorType: String,
                       errorMsg: String,
                       cause: String = "") extends Packet {

    def printError(): Unit = {
        def println(x: Any): Unit = Console.err.println(x)

        println(s"$errorType: $errorMsg")
        if (!cause.isBlank)
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

        private val TYPE = "[err]".getBytes
        private val ERROR_TYPE = "<type>".getBytes
        private val MSG = "<msg>".getBytes
        private val CAUSE = "<cause>".getBytes

        override def toBytes(implicit packet: ErrorPacket): Array[Byte] = {
            val errorType = packet.errorType.getBytes
            val errorMsg = packet.errorType.getBytes
            val cause = packet.errorType.getBytes
            TYPE ++ EmptyPacket.Factory.toBytesUnsigned(packet) ++
                    ERROR_TYPE ++ errorType ++
                    MSG ++ errorMsg ++
                    CAUSE ++ cause
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean =
            bytes.startsWith(TYPE)

        override def toPacket(implicit bytes: Array[Byte]): ErrorPacket = {
            val base = EmptyPacket.Factory.toPacket(bytes)
            val errorType = cutString(ERROR_TYPE, MSG)
            val msg = cutString(MSG, CAUSE)
            val cause = new String(cutEnd(CAUSE))
            ErrorPacket(base.channelID, base.senderIdentifier, base.targetIdentifier, errorType, msg, cause)
        }

    }

}
