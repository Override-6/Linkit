package fr.overridescala.vps.ftp.server.connection

import java.net.{Socket, SocketAddress}

import fr.overridescala.vps.ftp.api.packet.SimplePacketChannel
import fr.overridescala.vps.ftp.api.task.TasksHandler

case class RelayPointConnection protected(private[connection] var identifier: String,
                                          private val socket: Socket,
                                          private val tasksHandler: TasksHandler,
                                          address: SocketAddress) extends AutoCloseable {
    val packetChannel = new SimplePacketChannel(new ByteSocket(socket), identifier, socket.getRemoteSocketAddress, tasksHandler)
    val id: String = this.identifier

    private var isListening = false


    def startListening(onBytesReceived: Array[Byte] => Unit): Unit = {
        if (isListening)
            throw new IllegalAccessException("already listening !")
        val connectionThread = new Thread(() => listen(onBytesReceived))
        connectionThread.setName("Listen Thread for connection " + address)
        isListening = true
        connectionThread.start()
    }


    private def listen(onBytesReceived: Array[Byte] => Unit): Unit = {
        while (isListening) {
            val bytes = socket.getInputStream.readAllBytes()
            println(s"bytes = ${bytes.mkString("Array(", ", ", ")")}")
            onBytesReceived(bytes)
        }
    }

    override def close(): Unit = isListening = false
}
