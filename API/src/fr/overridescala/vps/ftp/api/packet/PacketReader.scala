package fr.overridescala.vps.ftp.api.packet

import fr.overridescala.vps.ftp.api.packet.ext.PacketManager


class PacketReader(socket: DynamicSocket){

    def readNextPacketBytes(): Array[Byte] = {
        val nextLength = nextPacketLength()
        if (nextLength == -1)
            return null

        val buff = new Array[Byte](nextLength)
        socket.read(buff)
        buff
    }

    private def nextPacketLength(): Int = {
        val buff = new Array[Byte](1)
        val packetSizeBytes = new Array[Byte](20)
        var i = 0
        while (!(buff sameElements PacketManager.SizeSeparator)) {
            val count = socket.read(buff)
            if (count < 0)
                return -1
            packetSizeBytes(i) = buff(0)
            i += 1
        }
        val sizeString = new String(packetSizeBytes.slice(0, i - 1))
        if (sizeString.trim.isEmpty)
            return -1
        sizeString.toInt
    }


}
