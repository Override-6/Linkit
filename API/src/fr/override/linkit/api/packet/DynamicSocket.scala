package fr.`override`.linkit.api.packet

import java.io._
import java.net.{ConnectException, InetSocketAddress, Socket}

import fr.`override`.linkit.api.exception.{RelayCloseException, RelayException}
import fr.`override`.linkit.api.system.event.EventObserver.EventNotifier
import fr.`override`.linkit.api.network.ConnectionState
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}
import fr.`override`.linkit.api.utils.ConsumerContainer

abstract class DynamicSocket(notifier: EventNotifier, autoReconnect: Boolean = true) extends JustifiedCloseable {

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


    def read(buff: Array[Byte]): Int = read(buff, 0)

    def read(buff: Array[Byte], pos: Int): Int = {
        locker.awaitConnected()
        ensureReady()
        try {
            val result = currentInputStream.read(buff, pos, buff.length - pos)
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
            if (closed || !autoReconnect)
                return -1
            handleReconnection()
            locker.markAsConnected()
            if (closed)
                return -1
            read(buff, pos)
        }

        -1
    }

    def read(length: Int): Array[Byte] = {
        val buff = new Array[Byte](length)
        var totalRead = 0
        while (totalRead != length) {
            val bytesRead = read(buff, totalRead)
            totalRead += bytesRead
        }
        buff
    }

    def write(buff: Array[Byte]): Unit = {
        locker.awaitConnected()
        ensureReady()
        locker.markAsWriting()
        try {
            currentOutputStream.write(buff)
            currentOutputStream.flush()
            //println(s"written : ${new String(buff)}")
        } catch {
            case e@(_: ConnectException | _: IOException) =>
                System.err.println(e.getMessage)
                if (closed || !autoReconnect)
                    return
                locker.markDisconnected()
                handleReconnection()
                locker.markAsConnected()
                write(buff)
        } finally {
            locker.unMarkAsWriting()
        }
    }

    def getState: ConnectionState = locker.getState

    def addConnectionStateListener(action: ConnectionState => Unit): Unit = locker.addStateListener(action)

    def isOpen: Boolean = !closed

    override def close(reason: CloseReason): Unit = {
        closed = true
        if (!currentSocket.isClosed)
            closeCurrentStreams()
    }

    protected def handleReconnection(): Unit

    protected def closeCurrentStreams(): Unit = {
        if (currentSocket == null)
            return
        locker.awaitRaise()
        currentSocket.close()
        currentInputStream.close()
        currentInputStream.close()
    }

    protected def markAsConnected(): Unit =
        locker.markAsConnected()

    private def ensureReady(): Unit = {
        if (closed) {
            throw new RelayCloseException("Socket closed")
        }
        if (currentInputStream == null || currentOutputStream == null) {
            throw new RelayException("Streams are not ready")
        }

    }

    import ConnectionState._
    private class SocketLocker {

        @volatile var isWriting = false
        @volatile var state: ConnectionState = ConnectionState.DISCONNECTED
        private val writeLock = new Object
        private val disconnectLock = new Object
        private val listeners = ConsumerContainer[ConnectionState]()

        def addStateListener(action: ConnectionState => Unit): Unit = {
            listeners += action
        }

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
            state = DISCONNECTED
            listeners.applyAll(state)

            notifier.onDisconnected()
        }

        def markAsConnected(): Unit = disconnectLock.synchronized {
            state = CONNECTED
            listeners.applyAll(state)

            disconnectLock.notifyAll()
            notifier.onConnected()
        }

        def awaitConnected(): Unit = disconnectLock.synchronized {
            if (state != CONNECTED) try {
                state = CONNECTING
                listeners.applyAll(state)

                disconnectLock.wait()
            } catch {
                case _: InterruptedException =>
            }
        }

        def getState: ConnectionState = state

    }

}
