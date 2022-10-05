/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.packet.traffic

import fr.linkit.api.gnom.network.ExternalConnectionState
import fr.linkit.api.internal.concurrency.packetWorkerExecution
import fr.linkit.api.internal.system._
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.internal.concurrency.pool.SimpleTaskController
import fr.linkit.engine.internal.util.{ConsumerContainer, NumberSerializer}

import java.io.{BufferedOutputStream, IOException, InputStream}
import java.net.{ConnectException, InetSocketAddress, Socket}
import java.nio.ByteBuffer

abstract class DynamicSocket(autoReconnect: Boolean = true) extends JustifiedCloseable {

    @volatile protected var currentSocket      : Socket               = _
    @volatile protected var currentOutputStream: BufferedOutputStream = _
    @volatile protected var currentInputStream : InputStream          = _


    private val listeners = ConsumerContainer[ExternalConnectionState]()

    def boundIdentifier: String

    def write(buff: ByteBuffer): Unit = {
        val array = new Array[Byte](buff.limit())
        val pos   = buff.position()
        buff.position(0)
        buff.get(array)
        buff.position(pos)
        write(array)
    }


    def write(buff: Array[Byte]): Unit = {
        //val t0 = System.currentTimeMillis()
        SocketLocker.awaitConnected()
        ensureReady()
        SocketLocker.markAsWriting()
        try {
            currentOutputStream.write(buff)
            currentOutputStream.flush()
            //val t1 = System.currentTimeMillis()
            //totalWriteTime += t1 - t0
            //NETWORK-DEBUG-MARK
            logUpload(boundIdentifier, buff)
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

    override def close(reason: Reason): Unit = {
        SocketLocker.markAsClosed()

        if (!currentSocket.isClosed)
            closeCurrentStreams()
        SocketLocker.releaseAllMonitors()
        AppLoggers.Traffic.info(s"All monitors that were waiting the socket that belongs to '$boundIdentifier' were been released")
    }

    override def isClosed: Boolean = SocketLocker.state == ExternalConnectionState.CLOSED

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
        val buff      = new Array[Byte](length)
        var totalRead = 0
        while (totalRead != length) {
            val bytesRead = read(buff, totalRead)
            totalRead += bytesRead
        }
        buff
    }

    def readInt(): Int = {
        val int = read(4)
        NumberSerializer.deserializeInt(int, 0)
    }

    def readShort(): Short = {
        val short = read(2)
        NumberSerializer.deserializeShort(short, 0)
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

    def addConnectionStateListener(@packetWorkerExecution callback: ExternalConnectionState => Unit): Unit = {
        listeners += callback
    }

    def getState: ExternalConnectionState = SocketLocker.state

    def remoteSocketAddress(): InetSocketAddress = {
        val inet = currentSocket.getInetAddress
        val port = currentSocket.getPort
        new InetSocketAddress(inet, port)
    }

    /**
     * Defines the algorithm that will handle the reconnection to the target.
     * If the method returns normally, that would mean the socket reconnection was made successfully.
     * For any exception, that make the reconnection impossible, the method may throw any exception.
     * */
    protected def handleReconnection(): Unit

    protected def markAsConnected(): Unit =
        SocketLocker.markAsConnected()

    private def ensureReady(): Unit = {
        if (currentInputStream == null || currentOutputStream == null) {
            throw new AppException("Streams are not ready")
        }

    }

    private def reconnect(): Unit = {
        AppLoggers.Traffic.warn(s"The connection with $boundIdentifier has been lost. Currently trying to reconnect...")
        SocketLocker.markAsConnecting()
        handleReconnection()
        AppLoggers.Traffic.info(s"The connection with $boundIdentifier has been reestablished.")
    }

    import ExternalConnectionState._

    private object SocketLocker {

        @volatile var isWriting                      = false
        @volatile var state: ExternalConnectionState = DISCONNECTED
        private val writeLock      = new Object
        private val disconnectLock = new SimpleTaskController()

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

        def markAsConnected(): Unit = {
            updateState(CONNECTED)

            disconnectLock.wakeupAllTasks()
        }

        def markAsConnecting(): Unit = {
            updateState(CONNECTING)
        }

        private def updateState(newState: ExternalConnectionState): Unit = {
            ensureReady()
            if (state == newState)
                return
            if (isClosed)
                throw new SocketClosedException("This socket is definitely closed.")

            listeners.applyAll(newState)
            state = newState
        }

        def awaitConnected(): Unit = {
            if (state == CLOSED)
                throw new IllegalCloseException("Attempted to wait this socket to be connected again, but it is now closed.")
            if (state != CONNECTED) try {
                AppLoggers.Traffic.trace(s"The socket is currently waited by thread '${Thread.currentThread()}' because the connection with $boundIdentifier isn't ready or is disconnected.")
                disconnectLock.pauseTask()
                AppLoggers.Traffic.trace(s"The connection with $boundIdentifier is now ready.")
            } catch {
                case _: InterruptedException =>
            }
        }

        def releaseAllMonitors(): Unit = {
            writeLock.synchronized {
                writeLock.notifyAll()
            }
            disconnectLock.wakeupAllTasks()
        }

        def markAsClosed(): Unit = {
            updateState(CLOSED)
        }

    }

    private def logUpload(target: String, bytes: Array[Byte]): Unit = if (AppLoggers.Traffic.isTraceEnabled) {
        val preview       = new String(bytes.take(1000)).replace('\n', ' ').replace('\r', ' ')
        val packetOrdinal = if (bytes.length > 10) NumberSerializer.deserializeInt(bytes, 2) else -1
        val length        = if (bytes.length > 10) NumberSerializer.deserializeInt(bytes, 6) else bytes.length
        AppLoggers.Traffic.trace(s"${Console.MAGENTA}Written : ↑ $target ↑ (len: $length, ord: $packetOrdinal) $preview")
    }

}
