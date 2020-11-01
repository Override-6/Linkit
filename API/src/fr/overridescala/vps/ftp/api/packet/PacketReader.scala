package fr.overridescala.vps.ftp.api.packet

import java.io.{BufferedInputStream, Closeable}
import java.net.Socket
import java.util.Optional

import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException
import fr.overridescala.vps.ftp.api.packet.ext.PacketManager
import fr.overridescala.vps.ftp.api.utils.Constants


class PacketReader(socket: Socket) extends Closeable {

    private val input = new BufferedInputStream(socket.getInputStream)

    def readNextPacketBytes(): Array[Byte] = {
        val nextLength = nextPacketLength()
        if (nextLength == -1)
            return null

        val buff = new Array[Byte](nextLength)
        input.read(buff)
        buff
    }

    override def close(): Unit = input.close()

    private def nextPacketLength(): Int = {
        val buff = new Array[Byte](1)
        val packetSizeBytes = new Array[Byte](20)
        var i = 0
        while (!(buff sameElements PacketManager.SizeSeparator)) {
            val count = input.read(buff)
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
