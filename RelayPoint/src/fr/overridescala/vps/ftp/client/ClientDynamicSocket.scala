package fr.overridescala.vps.ftp.client

import java.io._
import java.net.{InetSocketAddress, Socket, SocketAddress}

import fr.overridescala.vps.ftp.api.packet.DynamicSocket

import scala.annotation.tailrec

class ClientDynamicSocket(boundAddress: InetSocketAddress) extends DynamicSocket {

    private var currentSocket: Socket = _
    private var currentOutputStream: OutputStream = _
    private var currentInputStream: InputStream = _

    make(() => {
        newSocket()
    })

    override def remoteSocketAddress(): InetSocketAddress = {
        val inet = currentSocket.getInetAddress
        val port = currentSocket.getPort
        new InetSocketAddress(inet, port)
    }

    override def close(): Unit = {
        if (currentSocket == null || currentSocket.isClosed)
            return
        currentSocket.close()
        currentOutputStream.close()
        currentInputStream.close()
    }

    override def read(buff: Array[Byte]): Int = make(() => {
        currentInputStream.read(buff)
    })

    override def write(buff: Array[Byte]): Unit = make(() => {
        currentOutputStream.write(buff)
        currentOutputStream.flush()
    })

    private def newSocket(): Unit = {
        close()
        currentSocket = new Socket(boundAddress.getAddress, boundAddress.getPort)
        currentOutputStream = new BufferedOutputStream(currentSocket.getOutputStream)
        currentInputStream = new BufferedInputStream(currentSocket.getInputStream)
    }

    @tailrec
    private def make[T](action: () => T): T = {
        try {
            action()
        } catch {
            case _: IOException =>
                println(s"Socket disconnected from ${boundAddress.getHostString}")
                println("Starting reconnection in 5 seconds...")
                Thread.sleep(5000)
                println("Reconnecting...")
                newSocket()
                println("Reconnected !")
                make(action)
        }
    }
}
