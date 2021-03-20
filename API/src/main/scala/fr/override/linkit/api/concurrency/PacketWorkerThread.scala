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

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.concurrency.PacketWorkerThread.packetReaderThreadGroup
import fr.`override`.linkit.api.exception.{IllegalPacketWorkerLockException, IllegalThreadException}
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}

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
                Relay.Log.error("Packet reading threw an error", e)
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
    @relayWorkerExecution
    protected def refresh(): Unit

    setDaemon(true)

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
     * @throws IllegalThreadException
     * */
    def safeLock(anyRef: AnyRef, timeout: Long = 0): Unit = {
        checkNotCurrent()
        anyRef.synchronized {
            anyRef.wait(timeout)
        }
    }

}
