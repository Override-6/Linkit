package fr.overridescala.vps.ftp.api.packet

import java.io.{BufferedInputStream, Closeable}
import java.net.Socket
import java.util.Optional

import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException
import fr.overridescala.vps.ftp.api.packet.ext.PacketManager


class PacketReader(socket: Socket, packetHandler: PacketManager) extends Closeable {


    private val input = new BufferedInputStream(socket.getInputStream)

    def readPacket(): Optional[Packet] = {
        val bytes = input.readAllBytes()
        try {
            return Optional.of(packetHandler.toPacket(bytes))
        } catch {
            case e: UnexpectedPacketException => e.printStackTrace()
        }
        Optional.empty()
    }

    override def close(): Unit = input.close()


}
