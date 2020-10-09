package fr.overridescala.vps.ftp.api.packet.ext.fundamental

import fr.overridescala.vps.ftp.api.packet.Packet
import fr.overridescala.vps.ftp.api.packet.ext.PacketFactory

case class ErrorPacket(errorType: String,
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

    object Factory extends PacketFactory[ErrorPacket] {

        private val TYPE = "[err]".getBytes
        private val MSG = "<msg>".getBytes
        private val CAUSE = "<cause>".getBytes

        override def toBytes(implicit packet: ErrorPacket): Array[Byte] = {
            TYPE ++ packet.errorType.getBytes ++
                    MSG ++ packet.errorMsg.getBytes ++
                    CAUSE ++ packet.cause.getBytes
        }

        override def canTransform(implicit bytes: Array[Byte]): Boolean =
            bytes.startsWith(TYPE)

        override def toPacket(implicit bytes: Array[Byte]): ErrorPacket = {
            val errorType = cutString(TYPE, MSG)
            val errorMsg = cutString(MSG, CAUSE)
            val cause = new String(cutEnd(CAUSE))
            ErrorPacket(errorType, errorMsg, cause)
        }

    }
}
