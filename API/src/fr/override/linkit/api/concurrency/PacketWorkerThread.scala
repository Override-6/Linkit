/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.api.concurrency

import fr.`override`.linkit.api.concurrency.PacketWorkerThread.packetReaderThreadGroup
import fr.`override`.linkit.api.exception.{IllegalPacketWorkerLockException, IllegalThreadException}
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}

import scala.util.control.NonFatal

abstract class PacketWorkerThread extends Thread(packetReaderThreadGroup, "Packet Read Worker") with JustifiedCloseable {

    private var open = true

    override def isClosed: Boolean = open

    override def run(): Unit = {
        try {
            while (open) {
                //println("Waiting for next packet...")
                refresh()
            }
        } catch {
            case NonFatal(e) =>
                e.printStackTrace()
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
     * This methods reads and handle any packet that comes from a socket.
     * The method may not throw any exception. if it is, this packet worker thread will
     * stop !
     * */
    @relayWorkerExecution
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

    def checkCurrent(): Unit = {
        if (!isCurrentWorkerThread)
            throw new IllegalThreadException("This action must be performed in a Packet Worker thread !")
    }

    def checkNotCurrent(): Unit = {
        if (isCurrentWorkerThread)
            throw new IllegalThreadException("This action must not be performed in a Packet Worker thread !")
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
