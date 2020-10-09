package fr.overridescala.vps.ftp.api.packet

import java.io.{BufferedInputStream, Closeable}
import java.net.Socket
import java.util.Optional

import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException
import fr.overridescala.vps.ftp.api.packet.ext.PacketManager
import fr.overridescala.vps.ftp.api.utils.Constants


class PacketReader(socket: Socket, packetHandler: PacketManager) extends Closeable {


    private val input = new BufferedInputStream(socket.getInputStream)

    def readPacket(): Optional[Packet] = {
        val bytes = input.readNBytes(nextPacketLength())
        try {
            return Optional.of(packetHandler.toPacket(bytes))
        } catch {
            case e: UnexpectedPacketException => e.printStackTrace()
        }
        Optional.empty()
    }


    override def close(): Unit = input.close()

    private def nextPacketLength(): Int = {
        val buff = new Array[Byte](1)
        val packetSizeBytes = new Array[Byte](Constants.MAX_PACKET_LENGTH.toString.length)
        var i = 0
        do {
            input.read(buff)
            packetSizeBytes(i) = buff(0)
            i += 1
        } while (new String(buff) != ":")
        new String(packetSizeBytes.slice(0, i - 1)).toInt
    }


}
