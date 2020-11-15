package fr.overridescala.vps.ftp.api.packet

import java.io._
import java.net.{ConnectException, InetSocketAddress, Socket}

import fr.overridescala.vps.ftp.api.`extension`.event.EventDispatcher.EventNotifier
import fr.overridescala.vps.ftp.api.system.{Reason, RelayCloseable}

abstract class DynamicSocket(notifier: EventNotifier) extends RelayCloseable {

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
            case e@(_: ConnectException | _: IOException) =>
                System.err.println(e.getMessage)
                return onDisconnect()
        }

        def onDisconnect(): Int = {
            locker.markDisconnected()
            if (closed)
                return -1
            handleReconnection()
            locker.markAsConnected()
            if (closed)
                return -1
            read(buff)
        }

        -1
    }

    def write(buff: Array[Byte]): Unit = {
        locker.awaitConnected()
        ensureOpen()
        try {
            locker.markAsWriting()
            currentOutputStream.write(buff)
            currentOutputStream.flush()
        } catch {
            case e@(_: ConnectException | _: IOException) =>
                System.err.println(e.getMessage)
                if (closed)
                    return
                locker.markDisconnected()
                handleReconnection()
                locker.markAsConnected()
                write(buff)
        } finally {
            locker.unMarkAsWriting()
        }
    }

    def isConnected: Boolean = !locker.isDisconnected

    def isOpen: Boolean = !closed

    override def close(reason: Reason): Unit = {
        if (currentSocket.isClosed) {
            closed = true
            return
        }
        closed = true
        closeCurrentStreams()
    }

    protected def handleReconnection(): Unit

    protected def closeCurrentStreams(): Unit = {
        if (currentSocket == null)
            return
        locker.awaitRaise()
        currentSocket.close()
    }

    protected def markAsConnected(connected: Boolean): Unit =
        if (connected) locker.markAsConnected() else locker.markDisconnected()

    private def ensureOpen(): Unit = {
        if (closed)
            throw new UnsupportedOperationException("Socket closed.")
    }

    private class SocketLocker {
        @volatile var isWriting = false
        @volatile var isDisconnected = true
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
            notifier.onDisconnected()
        }

        def markAsConnected(): Unit = disconnectLock.synchronized {
            isDisconnected = false
            disconnectLock.notifyAll()
            notifier.onConnected()
        }

        def awaitConnected(): Unit = disconnectLock.synchronized {
            if (isDisconnected)
                disconnectLock.wait()
        }

    }

}
