package fr.overridescala.vps.ftp.api.packet

import java.net.Socket

import fr.overridescala.vps.ftp.api.utils.Constants

class PacketReader(socket: Socket) {


    private val input = socket.getInputStream
    private val buff = new Array[Byte](1)

    def readPacket(): Packet = {
        val flagSize = Protocol.PACKET_SIZE_FLAG.length + Constants.MAX_PACKET_LENGTH.toString.length
        val packetSizeBytes = new Array[Byte](flagSize)
        var i = 0

        def flagNotFound: Boolean =
            !packetSizeBytes.containsSlice(")") || !packetSizeBytes.containsSlice(Protocol.PACKET_SIZE_FLAG)

        while (flagNotFound) {
            input.read(buff)
            packetSizeBytes(i) = buff(0)
            i += 1
        }
        val packetBytes = input.readNBytes(Protocol.getFirstPacketLength(packetSizeBytes))
        Protocol.toPacket(packetBytes)
    }


}
