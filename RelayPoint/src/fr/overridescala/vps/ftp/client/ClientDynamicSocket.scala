package fr.overridescala.vps.ftp.client

import java.io._
import java.net.{ConnectException, InetSocketAddress, Socket}

import fr.overridescala.vps.ftp.api.packet.DynamicSocket

class ClientDynamicSocket(boundAddress: InetSocketAddress) extends DynamicSocket {

    private var currentSocket: Socket = _
    private var currentOutputStream: OutputStream = _
    private var currentInputStream: InputStream = _

    private val AttemptSleepTime = 5000

    make {
        newSocket()
        while (currentSocket == null) {
            newSocket()
            Thread.sleep(AttemptSleepTime)
        }
    }

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

    override def read(buff: Array[Byte]): Int = make {
        currentInputStream.read(buff)
    }

    override def write(buff: Array[Byte]): Unit = make {
        currentOutputStream.write(buff)
        currentOutputStream.flush()
    }

    private def newSocket(): Unit = {
        close()
        currentSocket = new Socket(boundAddress.getAddress, boundAddress.getPort)
        currentOutputStream = new BufferedOutputStream(currentSocket.getOutputStream)
        currentInputStream = new BufferedInputStream(currentSocket.getInputStream)
    }

    private def make[T](action: => T): T = {
        try {
            action
        } catch {
            case _: IOException | _: ConnectException =>
                println(s"Socket disconnected from ${boundAddress.getAddress.getHostAddress} or unable to connect")
                println(s"Reconnecting in $AttemptSleepTime ms...")
                Thread.sleep(AttemptSleepTime)
                make {
                    println("Reconnecting...")
                    newSocket()
                    println("Reconnected !")
                    action
                }
        }
    }
}
