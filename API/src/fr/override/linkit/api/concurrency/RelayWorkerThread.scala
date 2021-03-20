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

import fr.`override`.linkit.api.concurrency.RelayWorkerThread.factory

import java.util.concurrent.{Executors, ThreadFactory}
import scala.util.control.NonFatal

class RelayWorkerThread() extends AutoCloseable {

    private val executor = Executors.newFixedThreadPool(3, factory)
    private var closed = false

    def runLater(action: Unit => Unit): Unit = {
        if (!closed)
            executor.submit((() => {
                try
                    action(null)
                catch {
                    case NonFatal(e) => e.printStackTrace()
                }
            }): Runnable)
    }

    override def close(): Unit = {
        closed = true
        executor.shutdownNow()
    }

}

object RelayWorkerThread {

    val workerThreadGroup: ThreadGroup = new ThreadGroup("Relay Worker")
    private var activeCount = 0
    val factory: ThreadFactory = new Thread(workerThreadGroup, _, "Relay Worker Thread-" + {
        activeCount += 1;
        activeCount
    })

    def checkCurrentIsWorker(): Unit = {
        if (!isCurrentWorkerThread)
            throw new IllegalStateException("This action must be performed in a Packet Worker thread !")
    }

    def checkCurrentIsNotWorker(): Unit = {
        if (isCurrentWorkerThread)
            throw new IllegalStateException("This action must not be performed in a Packet Worker thread !")
    }

    def isCurrentWorkerThread: Boolean = {
        Thread.currentThread().getThreadGroup == workerThreadGroup
    }

    def safeLock(anyRef: AnyRef, timeout: Long = 0): Unit = {
        checkCurrentIsNotWorker()
        anyRef.synchronized {
            anyRef.wait(timeout)
        }
    }
}
