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
            throw new NoSuchPacketException("this PacketLoader doesn't have any packet in his buffer")
        val bytes = buffer.array()
        val packetLength = Protocol.getPacketLength(bytes)
        val bytesToKeep = bytes.slice(packetLength, buffer.position())
        buffer = ByteBuffer.allocate(Constants.MAX_PACKET_LENGTH * 3)
        buffer.put(bytesToKeep)
        Protocol.toPacket(bytes)
    }

    def isPacketPresent: Boolean = {
        Protocol.containsPacket(buffer.array())
    }


}
