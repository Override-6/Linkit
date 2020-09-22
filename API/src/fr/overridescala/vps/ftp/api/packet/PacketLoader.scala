package fr.overridescala.vps.ftp.api.packet

import java.nio.ByteBuffer

import fr.overridescala.vps.ftp.api.utils.Constants

class PacketLoader {

    private val buffer = ByteBuffer.allocateDirect(Constants.MAX_PACKET_LENGTH * 3)

    def add(bytes: Array[Byte]): Unit = {
        //println(s"bytes.length = ${bytes.length}")
        //println(s"buffer.limit() = ${buffer.limit()}")

        buffer.put(bytes)
    }

    def nextPacket: DataPacket = {
        if (!isPacketPresent)
            return null
        val bytes = getBytes()
        val packetLength = Protocol.getPacketLength(bytes)
        val packetBytes = bytes.slice(0, packetLength)
        val bytesToKeep = bytes.slice(packetLength, buffer.position())
        buffer.position(0)
        buffer.put(bytesToKeep)

        Protocol.toPacket(packetBytes)
    }

    private def isPacketPresent: Boolean = {
        Protocol.containsPacket(getBytes())
    }

    private def getBytes(): Array[Byte] = {
        val array = new Array[Byte](buffer.position())
        val pos = buffer.position()
        buffer.position(0).get(array)
        buffer.position(pos)
        array
    }

}
