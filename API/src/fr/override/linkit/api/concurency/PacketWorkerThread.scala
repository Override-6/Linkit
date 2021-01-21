package fr.`override`.linkit.api.concurency

import fr.`override`.linkit.api.concurency.PacketWorkerThread.packetReaderThreadGroup
import fr.`override`.linkit.api.exception.IllegalPacketWorkerLockException
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}

abstract class PacketWorkerThread extends Thread(packetReaderThreadGroup, "Packet Read Worker") with JustifiedCloseable {

    private var open = true

    override def isClosed: Boolean = open

    override def run(): Unit = {
        while (open) {
            readAndHandleOnePacket()
        }
    }

    override def close(reason: CloseReason): Unit = {
        open = false
        interrupt()
    }

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
