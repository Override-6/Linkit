package fr.overridescala.vps.ftp.api.packet

import java.nio.ByteBuffer

import fr.overridescala.vps.ftp.api.exceptions.NoSuchPacketException
import fr.overridescala.vps.ftp.api.utils.Constants

class PacketLoader {

    private var buffer = ByteBuffer.allocate(Constants.MAX_PACKET_LENGTH * 3)

    def add(bytes: Array[Byte]): Unit = {
        buffer.put(bytes)
    }

    def nextPacket: DataPacket = {
        if (!isPacketPresent)
            return null
        val bytes = buffer.array()
        val packetLength = Protocol.getPacketLength(bytes)
        val packetBytes = bytes.slice(0, packetLength)
        val bytesToKeep = bytes.slice(packetLength, buffer.position())
        buffer = ByteBuffer.allocate(Constants.MAX_PACKET_LENGTH * 3)
        buffer.put(bytesToKeep)

        Protocol.toPacket(packetBytes)
    }

    private def isPacketPresent: Boolean = {
        Protocol.containsPacket(buffer.array())
    }


}
