/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.core.local.concurrency.pool

import fr.linkit.api.local.concurrency.IllegalThreadException

import scala.collection.mutable

class ReservedWorkerController extends AbstractWorkerController[BusyWorkerThread]() {

    private val pools = mutable.HashSet.empty[BusyWorkerPool]

    override def currentWorker: BusyWorkerThread = BusyWorkerPool.currentWorker

    override def notifyWorker(worker: BusyWorkerThread, taskID: Int): Unit = BusyWorkerPool.notifyTask(worker, taskID)

    override def waitCurrentTask(): Unit = {
        getCurrentPool.waitCurrentTask()
    }

    override def waitCurrentTask(millis: Long): Unit = {
        getCurrentPool.waitCurrentTaskForAtLeast(millis)
    }

    def addPool(pool: BusyWorkerPool): Unit = pools += pool

    def removePool(pool: BusyWorkerPool): Unit = pools -= pool

    private def getCurrentPool: BusyWorkerPool = {
        val worker = currentWorker
        val workerPool = worker.pool
        if (!pools.contains(workerPool))
            throw IllegalThreadException("This thread's pool is not whitelisted to this worker controller.")
        workerPool
    }

}
