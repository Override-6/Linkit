package fr.overridescala.vps.ftp.api.packet

import java.nio.ByteBuffer

import fr.overridescala.vps.ftp.api.exceptions.NoSuchPacketException
import fr.overridescala.vps.ftp.api.utils.Constants

class PacketLoader {

    private var buffer = ByteBuffer.allocate(Constants.MAX_PACKET_LENGTH * 3)

    def add(bytes: Array[Byte]): Unit = {
        buffer.put(bytes)
    }

    def retrievePacket: DataPacket = {
        if (!isPacketPresent)
            throw new NoSuchPacketException("this Packet")
        val bytes = buffer.array()
        buffer = ByteBuffer.allocate(Constants.MAX_PACKET_LENGTH * 3)
        Protocol.toPacket(bytes)
    }

    def isPacketPresent: Boolean = {
        Protocol.containsPacket(buffer.array())
    }


}
