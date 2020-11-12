package fr.overridescala.vps.ftp.api.packet

import java.io._
import java.net.{ConnectException, InetSocketAddress, Socket}

abstract class DynamicSocket extends Closeable {

    @volatile protected var currentSocket: Socket = _
    @volatile protected var currentOutputStream: BufferedOutputStream = _
    @volatile protected var currentInputStream: InputStream = _

    @volatile private var closed = false

    private val locker = new SocketLocker()

    def remoteSocketAddress(): InetSocketAddress = {
        val inet = currentSocket.getInetAddress
        val port = currentSocket.getPort
        new InetSocketAddress(inet, port)
    }


    def read(buff: Array[Byte]): Int = {
        locker.awaitConnected()
        ensureOpen()
        try {
            val result = currentInputStream.read(buff)
            if (result < 0)
                return onDisconnect()
            return result
        } catch {
            case _@(_: ConnectException | _: IOException) =>
                return onDisconnect()
        }

        def onDisconnect(): Int = {
            locker.markDisconnected()
            handleReconnection()
            locker.markAsConnected()
            read(buff)
        }
        -1
    }

    def write(buff: Array[Byte]): Unit = {
        locker.awaitConnected()
        ensureOpen()
        try {
            locker.markAsWriting()
            try {
                currentOutputStream.write(buff)
                currentOutputStream.flush()
            } finally {
                locker.unMarkAsWriting()
            }
        } catch {
            case _@(_: ConnectException | _: IOException) =>
                if (closed)
                    return Option.empty
                locker.markDisconnected()
                handleReconnection()
                locker.markAsConnected()
                write(buff)
        }
    }

    def isConnected: Boolean = !locker.isDisconnected

    override def close(): Unit = {
        //FIXME removed closed flag for debug only
        if (currentSocket.isClosed) {
            //closed = true
            return
        }
        //closed = true
        closeCurrentStreams()
    }

    protected def handleReconnection(): Unit

    protected def closeCurrentStreams(): Unit = {
        if (currentSocket == null)
            return
        locker.awaitRaise()
        currentSocket.close()
    }

    private def ensureOpen(): Unit = {
        if (closed)
            throw new UnsupportedOperationException("Socket closed.")
    }

    private class SocketLocker {
        @volatile var isWriting = false
        @volatile var isDisconnected = false
        private val writeLock = new Object
        private val disconnectLock = new Object

        def markAsWriting(): Unit = {
            isWriting = true
        }

        def unMarkAsWriting(): Unit = writeLock.synchronized {
            isWriting = false
            writeLock.notifyAll()
        }

        def awaitRaise(): Unit = writeLock.synchronized {
            if (isWriting)
                writeLock.wait()
        }

        def markDisconnected(): Unit = {
            isDisconnected = true
        }

        def markAsConnected(): Unit = disconnectLock.synchronized {
            isDisconnected = false
            disconnectLock.notifyAll()
        }

        def awaitConnected(): Unit = disconnectLock.synchronized {
            if (isDisconnected)
                disconnectLock.wait()
        }

    }

}
