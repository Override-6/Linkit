package fr.overridescala.vps.ftp.api.packet

import scala.util.control.NonFatal


class PacketReader(socket: DynamicSocket) {

    def readNextPacketBytes(): Array[Byte] = synchronized {
        val nextLength = nextPacketLength()
        if (nextLength == -1 || !socket.isOpen)
            return null

        val bytes = socket.read(nextLength)
        bytes
    }

    private def nextPacketLength(): Int = {
        try {
            val int = new String(socket.read(1))
            val packetLengthFlagLength = Integer.parseInt(int, 16)
            Integer.parseInt(new String(socket.read(packetLengthFlagLength)))
        } catch {
            case NonFatal(e) =>
                Console.err.println(e.getMessage)
                System.exit(0)
                -1
        }
    }


}
