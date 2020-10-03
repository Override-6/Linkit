package fr.overridescala.vps.ftp.api.packet

import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel

class SocketWriter private(socket: Socket) extends WritableByteChannel {

    private val out = socket.getOutputStream

    override def write(src: ByteBuffer): Int = {
        val bytes = getBytes(src)
        out.write(bytes)
        bytes.length
    }

    override def isOpen: Boolean = !socket.isClosed

    override def close(): Unit = socket.close()


    private def getBytes(buffer: ByteBuffer): Array[Byte] = {
        if (buffer.hasArray)
            return buffer.array()

        val array = new Array[Byte](buffer.position())
        val pos = buffer.position()
        buffer.position(0).get(array)
        buffer.position(pos)
        array
    }

}

object SocketWriter {
    def wrap(socket: Socket): SocketWriter =
        new SocketWriter(socket)
}
