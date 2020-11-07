package fr.overridescala.vps.ftp.server.connection

import java.io.{BufferedInputStream, BufferedOutputStream, InputStream, OutputStream}
import java.net.{Socket, SocketAddress}

import fr.overridescala.vps.ftp.api.packet.DynamicSocket

class SocketContainer() extends DynamicSocket {

    private var currentSocket: Socket = _
    private var currentOutputStream: OutputStream = _
    private var currentInputStream: InputStream = _

    override def remoteSocketAddress(): SocketAddress = currentSocket.getRemoteSocketAddress

    override def read(buff: Array[Byte]): Int = make(() => {
        currentInputStream.read(buff)
    })

    override def write(buff: Array[Byte]): Unit = make(() => {
        currentOutputStream.write(buff)
        currentOutputStream.flush()
    })


    def set(socket: Socket): Unit = synchronized {
        currentSocket = socket
        currentOutputStream = new BufferedOutputStream(currentSocket.getOutputStream)
        currentInputStream = new BufferedInputStream(currentSocket.getInputStream)
        notifyAll()
    }

    override def close(): Unit = {
        currentSocket.close()
        currentOutputStream.close()
        currentInputStream.close()
    }

    private def make[T](action: () => T): T = synchronized {
        if (currentSocket.isConnected) {
            action()
        } else {
            println(s"Socket disconnected from ${remoteSocketAddress()}")
            println("Starting reconnection in 5 seconds...")
            Thread.sleep(5000)
            println("Reconnecting...")
            wait()
            println("Reconnected !")
            action()
        }
    }
}
