package fr.`override`.linkit.api.concurrency

import fr.`override`.linkit.api.concurrency.PacketWorkerThread.packetReaderThreadGroup
import fr.`override`.linkit.api.exception.IllegalPacketWorkerLockException
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}

import scala.util.control.NonFatal

abstract class PacketWorkerThread extends Thread(packetReaderThreadGroup, "Packet Read Worker") with JustifiedCloseable {

    private var open = true

    override def isClosed: Boolean = open

    override def run(): Unit = {
        try {
            while (open) {
                readAndHandleOnePacket()
            }
        } catch {
            case NonFatal(e) =>
                e.printStackTrace()
                open = false
        }
    }

    override def close(reason: CloseReason): Unit = {
        open = false
        //The stop is mandatory here because, if we interrupt, the thread will continue reading socket despite the request
        //Interrupting could cause some problems in a relay closing context, because the socket that this thread handles will
        //try to reconnect.
        //FIXME replace deprecated stop method with interrupt
        stop()
    }

    /**
     * This methods reads and handle any packet that comes from a socket.
     * The method may not throw any exception. if it is, this packet worker thread will
     * stop !
     * */
    protected def readAndHandleOnePacket(): Unit

}

object PacketWorkerThread {

    /**
     * Packet Worker Threads have to be registered in this ThreadGroup in order to throw an exception when a relay worker thread
     * is about to be locked by a monitor, that concern packet reception (example: lockers of BlockingQueues in PacketChannels)
     *
     * @see [[IllegalPacketWorkerLockException]]
     * */
    val packetReaderThreadGroup: ThreadGroup = new ThreadGroup("Relay Packet Worker")

    def checkCurrent(): Unit = {
        if (!isCurrentWorkerThread)
            throw new IllegalStateException("This action must be performed in a Packet Worker thread !")
    }

    def checkNotCurrent(): Unit = {
        if (isCurrentWorkerThread)
            throw new IllegalStateException("This action must not be performed in a Packet Worker thread !")
    }

    def currentThread(): Option[PacketWorkerThread] = {
        Thread.currentThread() match {
            case worker: PacketWorkerThread => Some(worker)
            case _ => None
        }
    }

    def isCurrentWorkerThread: Boolean = {
        currentThread().isDefined
    }

    def safeLock(anyRef: AnyRef, timeout: Long = 0): Unit = {
        checkNotCurrent()
        anyRef.synchronized {
            anyRef.wait(timeout)
        }
    }

}
