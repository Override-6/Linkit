package fr.overridescala.vps.ftp.api.packet

import java.nio.ByteBuffer

import fr.overridescala.vps.ftp.api.utils.Constants
import org.jetbrains.annotations.Nullable

/**
 * when a packet is sent, if his target have a worse connection than his sender, the packet may be downloaded in fragments.
 * To resolve this problem, this class stores downloaded bytes and retrieve packet from buffered bytes sequence.
 * */
class PacketLoader {

    /**
     * downloaded bytes are stored here.
     * */
    private val buffer = ByteBuffer.allocateDirect(Constants.MAX_PACKET_LENGTH * 10)

    /**
     * add downloaded bytes to the buffer
     *
     * @param bytes the bytes to store
     * */
    def add(bytes: Array[Byte]): Unit = {
        buffer.put(bytes)
    }

    /**
     * retrieves a [[DataPacket]] from the stored bytes
     *
     * @return a packet if found, null instead
     * */
    @Nullable def nextPacket: Packet = {
        if (!isPacketPresent)
            return null
        val bytes = getBytes
        val packetLength = Protocol.getFirstPacketLength(bytes)
        val packetBytes = bytes.slice(0, packetLength)
        val bytesToKeep = bytes.slice(packetLength, buffer.position())
        buffer.position(0)
        buffer.put(bytesToKeep)

        Protocol.toPacket(packetBytes)
    }

    /**
     * @return true if this packet loader contains packets to retrieve
     * */
    private def isPacketPresent: Boolean = {
        Protocol.containsPacket(getBytes)
    }

    private def getBytes: Array[Byte] = {
        val array = new Array[Byte](buffer.position())
        val pos = buffer.position()
        buffer.position(0).get(array)
        buffer.position(pos)
        array
    }

}
