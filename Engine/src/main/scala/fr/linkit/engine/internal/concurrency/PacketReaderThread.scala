/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.concurrency

import fr.linkit.api.gnom.packet.traffic.PacketReader
import fr.linkit.api.gnom.persistence.PacketDownload
import fr.linkit.api.internal.concurrency.{IllegalThreadException, packetWorkerExecution}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.api.internal.system.{JustifiedCloseable, Reason}
import fr.linkit.engine.internal.concurrency.PacketReaderThread.packetReaderThreadGroup

import java.nio.channels.AsynchronousCloseException
import scala.util.control.NonFatal

/**
 * A simple abstract class to easily handle packet reading.
 * */
class PacketReaderThread(reader: PacketReader,
                         bound: String) extends Thread(packetReaderThreadGroup, s"$bound's Read Worker") with JustifiedCloseable {

    private var open = true
    var onPacketRead   : PacketDownload => Unit = _ => ()
    var onReadException: () => Unit             = () => ()

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
                AppLoggers.Traffic.error("Packet reading threw an error")
                e.printStackTrace()
                open = false
        } finally {
            //println("STOPPED PACKET WORKER")
        }
    }

    override def close(reason: Reason): Unit = {
        open = false
        interrupt()
    }

    /**
     * This method must read and handle any packet that comes from a socket.
     * The method may not throw any exception. if it is, this packet worker thread will
     * stop !
     * */
    @packetWorkerExecution
    private def refresh(): Unit = {
        try {
            readNextPacket()
        } catch {
            case _: AsynchronousCloseException =>
                onException("Asynchronous close.")

            case NonFatal(e) =>
                onException(s"Suddenly disconnected from the server.")
                throw e
        }

        def onException(msg: String): Unit = {
            AppLoggers.Traffic.error(msg)
            onReadException()
        }
    }

    private def readNextPacket(): Unit = {
        reader.nextPacket(result => {
            onPacketRead(result)
        })
    }

}

object PacketReaderThread {

    /**
     * Packet Worker Threads have to be registered in this ThreadGroup in order to throw an exception when a worker thread
     * is about to be locked by a monitor, that concern packet reception (example: lockers of BlockingQueues in PacketChannels)
     *
     * @see [[IllegalPacketWorkerLockException]]
     * */
    val packetReaderThreadGroup: ThreadGroup = new ThreadGroup("Packet Reader")

    /**
     * ensures that the current thread is a [[PacketReaderThread]]
     *
     * @throws IllegalThreadException if the current thread is not a [[PacketReaderThread]]
     * */
    def checkCurrent(): Unit = {
        if (!isCurrentWorkerThread)
            throw IllegalThreadException("This action must be performed by a Packet Reader thread !")
    }

    /**
     * ensures that the current thread is not a [[PacketReaderThread]]
     *
     * @throws IllegalThreadException if the current thread is a [[PacketReaderThread]]
     * */
    def checkNotCurrent(): Unit = {
        if (isCurrentWorkerThread)
            throw new IllegalThreadException("This action must not be performed by a Packet Reader thread !")
    }

    /**
     * @return true if the current thread is an instance of [[PacketReaderThread]]
     * */
    def isCurrentWorkerThread: Boolean = {
        currentThread().isDefined
    }

    /**
     * Handles a lock if the current thread is not a [[PacketReaderThread]], otherwise, throw an [[IllegalThreadException]]
     * */
    def safeLock(anyRef: AnyRef, timeout: Int = 0): Unit = {
        checkNotCurrent()
        anyRef.synchronized {
            anyRef.wait(timeout)
        }
    }

    /**
     * @return an optional filled if the current thread is an instance of [[PacketReaderThread]]
     * */
    private def currentThread(): Option[PacketReaderThread] = {
        Thread.currentThread() match {
            case worker: PacketReaderThread => Some(worker)
            case _                          => None
        }
    }

}
