package fr.overridescala.vps.ftp.server.connection

import java.io.{BufferedInputStream, BufferedOutputStream, IOException, InputStream, OutputStream}
import java.net.{InetSocketAddress, Socket, SocketAddress}

import fr.overridescala.vps.ftp.api.packet.DynamicSocket

class SocketContainer() extends DynamicSocket {

    private var currentSocket: Socket = _
    private var currentOutputStream: OutputStream = _
    private var currentInputStream: InputStream = _

    override def remoteSocketAddress(): InetSocketAddress = {
        val inet = currentSocket.getInetAddress
        val port = currentSocket.getPort
        new InetSocketAddress(inet, port)
    }

    override def read(buff: Array[Byte]): Int = make(() => {
        currentInputStream.read(buff)
    })

    override def write(buff: Array[Byte]): Unit = synchronized {
        make(() => {
            currentOutputStream.write(buff)
            currentOutputStream.flush()
        })
    }


    def set(socket: Socket): Unit = synchronized {
        currentSocket = socket
        currentOutputStream = new BufferedOutputStream(currentSocket.getOutputStream)
        currentInputStream = new BufferedInputStream(currentSocket.getInputStream)
        notifyAll()
    }

    override def close(): Unit = {
        if (currentSocket.isClosed)
            return
        currentOutputStream.close()
        currentInputStream.close()
        currentSocket.close()
    }

    private def make[T](action: () => T): T = {
        try {
            action()
        } catch {
            case _: IOException =>
                println(s"Socket disconnected from ${remoteSocketAddress()}")
                println("Reconnecting...")
                //close()
                synchronized {
                    wait()
                }
                println("Reconnected !")
                make(action)
        }
    }
}