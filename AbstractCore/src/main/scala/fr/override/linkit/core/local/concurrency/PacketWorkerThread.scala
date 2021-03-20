package fr.`override`.linkit.core.local.concurrency

import fr.`override`.linkit.api.local.concurrency.{IllegalThreadException, workerExecution}
import fr.`override`.linkit.api.local.system.{CloseReason, JustifiedCloseable}
import fr.`override`.linkit.core.local.concurrency.PacketWorkerThread.packetReaderThreadGroup
import fr.`override`.linkit.core.local.system.ContextLogger

import scala.util.control.NonFatal

/**
 * A simple abstract class to easily handle packet reading.
 * */
abstract class PacketWorkerThread extends Thread(packetReaderThreadGroup, "Packet Read Worker") with JustifiedCloseable {

    private var open = true

    override def isClosed: Boolean = open

    /**
     * Simply calls the refresh method while the thread is open. <br>
     * The thread can only stop
     * */
    override def run(): Unit = {
        try {
            while (open) {
                //println("Waiting for next packet...")
                refresh()
            }
        } catch {
            case NonFatal(e) =>
                ContextLogger.error("Packet reading threw an error", e)
                open = false
        } finally {
            //println("STOPPED PACKET WORKER")
        }
    }

    override def close(reason: CloseReason): Unit = {
        open = false
        interrupt()
    }

    /**
     * This method must read and handle any packet that comes from a socket.
     * The method may not throw any exception. if it is, this packet worker thread will
     * stop !
     * */
    @workerExecution
    protected def refresh(): Unit

}

object PacketWorkerThread {

    /**
     * Packet Worker Threads have to be registered in this ThreadGroup in order to throw an exception when a relay worker thread
     * is about to be locked by a monitor, that concern packet reception (example: lockers of BlockingQueues in PacketChannels)
     *
     * @see [[IllegalPacketWorkerLockException]]
     * */
    val packetReaderThreadGroup: ThreadGroup = new ThreadGroup("Relay Packet Worker")

    /**
     * ensures that the current thread is a [[PacketWorkerThread]]
     * @throws IllegalThreadException if the current thread is not a [[PacketWorkerThread]]
     * */
    def checkCurrent(): Unit = {
        if (!isCurrentWorkerThread)
            throw new IllegalThreadException("This action must be performed in a Packet Worker thread !")
    }

    /**
     * ensures that the current thread is not a [[PacketWorkerThread]]
     * @throws IllegalThreadException if the current thread is a [[PacketWorkerThread]]
     * */
    def checkNotCurrent(): Unit = {
        if (isCurrentWorkerThread)
            throw new IllegalThreadException("This action must not be performed in a Packet Worker thread !")
    }

    /**
     * @return an optional filled if the current thread is an instance of [[PacketWorkerThread]]
     * */
    def currentThread(): Option[PacketWorkerThread] = {
        Thread.currentThread() match {
            case worker: PacketWorkerThread => Some(worker)
            case _ => None
        }
    }

    /**
     * @return true if the current thread is an instance of [[PacketWorkerThread]]
     * */
    def isCurrentWorkerThread: Boolean = {
        currentThread().isDefined
    }

    /**
     * Handles a lock if the current thread is a [[PacketWorkerThread]], otherwise, throw an [[IllegalThreadException]]
     * */
    def safeLock(anyRef: AnyRef, timeout: Long = 0): Unit = {
        checkNotCurrent()
        anyRef.synchronized {
            anyRef.wait(timeout)
        }
    }

}
