package fr.`override`.linkit.api.packet.traffic

import fr.`override`.linkit.api.Relay.Log
import fr.`override`.linkit.api.exception.{RelayCloseException, RelayException}
import fr.`override`.linkit.api.network.ConnectionState
import fr.`override`.linkit.api.network.ConnectionState.CLOSED
import fr.`override`.linkit.api.packet.serialization.NumberSerializer
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}
import fr.`override`.linkit.api.utils.ConsumerContainer

import java.io._
import java.net.{ConnectException, InetSocketAddress, Socket}

abstract class DynamicSocket(autoReconnect: Boolean = true) extends JustifiedCloseable {
    @volatile protected var currentSocket: Socket = _
    @volatile protected var currentOutputStream: BufferedOutputStream = _
    @volatile protected var currentInputStream: InputStream = _
    @volatile private var totalWriteTime: Long = 0

    def write(buff: Array[Byte]): Unit = {
        val t0 = System.currentTimeMillis()
        SocketLocker.awaitConnected()
        ensureReady()
        SocketLocker.markAsWriting()
        try {
            currentOutputStream.write(buff)
            currentOutputStream.flush()
            val t1 = System.currentTimeMillis()

            totalWriteTime += t1 - t0
            //NETWORK-DEBUG-MARK
            println(s"written : ${new String(buff.take(1000)).replace('\n', ' ').replace('\r', ' ')} (l: ${buff.length}) totalWriteTime: $totalWriteTime")
        } catch {
            case e@(_: ConnectException | _: IOException) =>

                if (e.getMessage.contains("socket write error")) {
                    e.printStackTrace()
                } else {
                    System.err.println(e)
                }

                if (isClosed || !autoReconnect)
                    return

                SocketLocker.markDisconnected()
                reconnect()
                SocketLocker.markAsConnected()
                write(buff)
        } finally {
            SocketLocker.unMarkAsWriting()
        }
    }

    override def close(reason: CloseReason): Unit = {
        SocketLocker.markAsClosed()

        if (!currentSocket.isClosed)
            closeCurrentStreams()
        SocketLocker.releaseAllMonitors()
        Log.trace(s"All monitors that were waiting the socket that belongs to '$boundIdentifier' were been released")
    }

    override def isClosed: Boolean = SocketLocker.state == CLOSED

    def read(buff: Array[Byte]): Int = read(buff, 0)

    protected def closeCurrentStreams(): Unit = {
        if (currentSocket == null)
            return
        SocketLocker.awaitRaise()

        currentSocket.close()
        currentInputStream.close()
        currentInputStream.close()
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

    def readInt(): Int = {
        NumberSerializer.deserializeInt(read(4), 0)
    }

    def read(buff: Array[Byte], pos: Int): Int = {
        SocketLocker.awaitConnected()
        ensureReady()
        try {
            val result = currentInputStream.read(buff, pos, buff.length - pos)
            if (result < 0) {
                return onDisconnect()
            }
            return result
        } catch {
            case e@(_: ConnectException | _: IOException) =>
                System.err.println(e.getMessage)
                return onDisconnect()
        }

        def onDisconnect(): Int = {
            SocketLocker.markDisconnected()
            if (isClosed || !autoReconnect)
                return -1
            reconnect()
            SocketLocker.markAsConnected()
            if (isClosed)
                return -1
            read(buff, pos)
        }

        -1
    }

    @volatile protected def boundIdentifier: String

    def getState: ConnectionState = SocketLocker.state

    def remoteSocketAddress(): InetSocketAddress = {
        val inet = currentSocket.getInetAddress
        val port = currentSocket.getPort
        new InetSocketAddress(inet, port)
    }

    def addConnectionStateListener(action: ConnectionState => Unit): Unit = SocketLocker.addStateListener(action)

    /**
     * Defines the algorithm that will handle the reconnection to the target.
     * If the method returns normally, that would mean the socket reconnection was made successfully.
     * For any exception, that make the reconnection impossible, the method may throw any exception.
     * */
    protected def handleReconnection(): Unit

    protected def markAsConnected(): Unit =
        SocketLocker.markAsConnected()

    private def ensureReady(): Unit = {
        if (isClosed) {
            throw new RelayCloseException("Socket closed")
        }
        if (currentInputStream == null || currentOutputStream == null) {
            throw new RelayException("Streams are not ready")
        }

    }

    private def reconnect(): Unit = {
        Log.warn(s"The connection with $boundIdentifier has been lost. Currently trying to reconnect...")
        handleReconnection()
        Log.trace(s"The connection with $boundIdentifier has been reestablished.")
    }

    import ConnectionState._

    private object SocketLocker {

        @volatile var isWriting = false
        @volatile var state: ConnectionState = DISCONNECTED
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
            updateState(DISCONNECTED)
        }

        def markAsConnected(): Unit = disconnectLock.synchronized {
            updateState(CONNECTED)

            disconnectLock.notifyAll()
        }

        private def updateState(newState: ConnectionState): Unit = {
            state = newState
            listeners.applyAll(state)
        }

        def awaitConnected(): Unit = disconnectLock.synchronized {
            if (state != CONNECTED) try {
                if (state == CLOSED)
                    throw new RelayCloseException("Attempted to wait this socket to be connected again, but it is now closed.")

                Log.warn(s"The socket is currently waiting on thread '${Thread.currentThread()}' because the connection with $boundIdentifier isn't ready or is disconnected.")
                disconnectLock.wait()
                Log.trace(s"The connection with $boundIdentifier is now ready.")
            } catch {
                case _: InterruptedException =>
            }
        }

        def releaseAllMonitors(): Unit = {
            writeLock.synchronized {
                writeLock.notifyAll()
            }
            disconnectLock.synchronized {
                disconnectLock.notifyAll()
            }
        }

        def markAsClosed(): Unit = {
            updateState(CLOSED)
        }

    }

}
