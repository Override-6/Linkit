package fr.`override`.linkit.api.packet.fundamental

import fr.`override`.linkit.api.packet.{Packet, PacketFactory, PacketTranslator}

case class ErrorPacket(errorType: String,
                       errorMsg: String,
                       cause: String = "") extends Packet {

    def printError(): Unit = {
        Console.err.println(s"$errorType: $errorMsg")
        if (!cause.isEmpty)
            Console.err.print(s"caused by: $cause ")
    }

}


object ErrorPacket extends PacketFactory[ErrorPacket] {

        import fr.`override`.linkit.api.packet.PacketUtils._

        private val TYPE = "[err]".getBytes
        private val MSG = "<msg>".getBytes
        private val CAUSE = "<cause>".getBytes

        override def decompose(translator: PacketTranslator)(implicit packet: ErrorPacket): Array[Byte] = {
            val errorType = packet.errorType.getBytes
            val errorMsg = packet.errorMsg.getBytes
            val cause = packet.cause.getBytes
            TYPE ++ errorType ++
                    MSG ++ errorMsg ++
                    CAUSE ++ cause
        }

        override def canTransform(translator: PacketTranslator)(implicit bytes: Array[Byte]): Boolean =
            bytes.startsWith(TYPE)

        override def build(translator: PacketTranslator)(implicit bytes: Array[Byte]): ErrorPacket = {
            val errorType = stringBetween(TYPE, MSG)
            val msg = stringBetween(MSG, CAUSE)
            val cause = new String(untilEnd(CAUSE))
            ErrorPacket(errorType, msg, cause)
        }

    override val packetClass: Class[ErrorPacket] = classOf[ErrorPacket]
}
