package fr.overridescala.vps.ftp.api.packet

import java.io.{BufferedInputStream, Closeable}
import java.net.Socket

import fr.overridescala.vps.ftp.api.packet.PacketReader.FLAG_SIZE
import fr.overridescala.vps.ftp.api.utils.Constants

import scala.collection.mutable.ListBuffer

object PacketReader {
    private val FLAG_SIZE = {
        val protocolFlagLength = Protocol.PACKET_SIZE_FLAG.length
        val maxPacketLength = Constants.MAX_PACKET_LENGTH.toString.getBytes.length
        val enclosingFlagLength = ")".getBytes.length
        protocolFlagLength + maxPacketLength + enclosingFlagLength
    }
}

class PacketReader(socket: Socket) extends Closeable {


    private val input = new BufferedInputStream(socket.getInputStream)
    private val buff = new Array[Byte](1)

    def readPacket(): Packet = {
        val packetSizeBytes = new Array[Byte](FLAG_SIZE)
        var i = 0

        def flagNotFound: Boolean =
            !packetSizeBytes.containsSlice(")") || !packetSizeBytes.containsSlice(Protocol.PACKET_SIZE_FLAG)

        do {
            input.read(buff)
            packetSizeBytes(i) = buff(0)
            i += 1
        } while (flagNotFound)

        val packetBytes = input.readNBytes(Protocol.getFirstPacketLength(packetSizeBytes))
        //println("found packet : " + new String(packetBytes))
        Protocol.toPacket(packetBytes)
    }

    override def close(): Unit = input.close()


}
