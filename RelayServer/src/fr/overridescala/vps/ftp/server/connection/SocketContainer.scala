package fr.overridescala.vps.ftp.server.connection

import java.io._
import java.net.{ConnectException, InetSocketAddress, Socket}

import fr.overridescala.vps.ftp.api.packet.DynamicSocket

class SocketContainer() extends DynamicSocket {

    private var currentSocket: Socket = _
    private var currentOutputStream: BufferedOutputStream = _
    private var currentInputStream: InputStream = _

    @volatile private var modCount = 1
    @volatile private var closed = false
    @volatile private var disconnectEvent: () => Unit = _

    private val writeLocker = new WriteLocker()

    override def remoteSocketAddress(): InetSocketAddress = {
        val inet = currentSocket.getInetAddress
        val port = currentSocket.getPort
        new InetSocketAddress(inet, port)
    }

    override def read(buff: Array[Byte]): Int = make {
        currentInputStream.read(buff)
    }.getOrElse(-1)

    override def write(buff: Array[Byte]): Unit = make {
        synchronized {
            writeLocker.setWrite()
            try {
                println(s"writeLocker.isWriting = ${writeLocker.isWriting}")
                currentOutputStream.write(buff)
                currentOutputStream.flush()
            } finally {
                writeLocker.raise()
                println(s"finally writeLocker.isWriting = ${writeLocker.isWriting}")
            }
        }
    }


    def isConnected: Boolean =
        currentSocket.isConnected

    def set(socket: Socket): Unit = synchronized {
        if (currentSocket != null)
            closeCurrentStreams()

        currentSocket = socket
        currentOutputStream = new BufferedOutputStream(currentSocket.getOutputStream)
        currentInputStream = new BufferedInputStream(currentSocket.getInputStream)
        println("Socket accepted " + remoteSocketAddress().getAddress.getHostName)
        notifyAll()
        modCount += 1
    }

    private[connection] def onDisconnected(action: => Unit): Unit = disconnectEvent = () => action

    override def close(): Unit = {
        if (currentSocket.isClosed) {
            closed = true
            return
        }
        closed = true
        closeCurrentStreams()
    }

    private def closeCurrentStreams(): Unit = {
        println(s"before writeLocker.isWriting = ${writeLocker.isWriting}")
        writeLocker.awaitRaise()

        println(s"after writeLocker.isWriting = ${writeLocker.isWriting}")

        currentOutputStream.close()
        currentInputStream.close()
        currentSocket.close()
    }

    private def make[T](action: => T): Option[T] = {
        ensureOpen()
        val prevModCount = modCount
        try {
            Option(action)
        } catch {
            case e@(_: ConnectException | _: IOException) =>
                System.err.println(e.getMessage)
                if (closed)
                    return Option.empty
                if (prevModCount == modCount)
                    handleReconnection()
                make(action)
        }
    }

    private def handleReconnection(): Unit = {
        println(s"Socket disconnected from ${remoteSocketAddress().getAddress.getHostAddress}")
        if (disconnectEvent != null)
            disconnectEvent()
        println("Reconnecting...")
        synchronized {
            wait()
        }
        println("Reconnected !")
    }

    private def ensureOpen(): Unit = {
        if (closed)
            throw new UnsupportedOperationException("Socket closed.")
    }

    private class WriteLocker {
        @volatile var isWriting = false

        def setWrite(): Unit = synchronized {
            isWriting = true
        }

        def raise(): Unit = synchronized {
            isWriting = false
            notifyAll()
        }

        def awaitRaise(): Unit = synchronized {
            if (isWriting)
                wait()
        }

    }

}