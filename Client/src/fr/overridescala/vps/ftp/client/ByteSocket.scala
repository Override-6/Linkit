package fr.overridescala.vps.ftp.client

import java.net.{InetSocketAddress, Socket}
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel

class ByteSocket(serverAddress: InetSocketAddress) extends ByteChannel {

    private val socket = new Socket(serverAddress.getAddress, serverAddress.getPort)

    override def read(dst: ByteBuffer): Int = {
        val bytes = new Array[Byte](dst.limit())
        val read = socket.getInputStream.read(bytes)
        dst.put(bytes)
        read
    }

    override def write(src: ByteBuffer): Int = {
        val bytes = new Array[Byte](src.limit())
        src.get(bytes)
        socket.getOutputStream.write(bytes)
        src.limit()
    }

    override def isOpen: Boolean = !socket.isClosed

    override def close(): Unit = socket.close()
}
