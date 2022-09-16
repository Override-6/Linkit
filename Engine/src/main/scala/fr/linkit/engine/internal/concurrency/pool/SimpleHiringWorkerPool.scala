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

package fr.linkit.engine.internal.concurrency.pool

import fr.linkit.api.internal.concurrency.IllegalThreadException
import fr.linkit.api.internal.concurrency.pool.{HiringWorkerPool, WorkerPools}

import java.util.concurrent.LinkedBlockingQueue

class SimpleHiringWorkerPool(name: String) extends AbstractWorkerPool(name) with HiringWorkerPool {

    private val workQueue = new LinkedBlockingQueue[Runnable]()
    
    override def nextTaskCount: Int = super.nextTaskCount
    
    override def hireCurrentThread(): Unit = {
        val currentWorker = WorkerPools.currentWorkerOpt
        if (currentWorker.isDefined)
            throw IllegalThreadException("could not hire current thread: current thread is a worker thread")

        val currentThread = Thread.currentThread()
        val worker        = new HiredWorker(currentThread, this)
        EngineWorkerPools.bindWorker(currentThread, worker)
        addWorker(worker)
    }

    override protected def post(runnable: Runnable): Unit = workQueue.put(runnable)

    override protected def countRemainingTasks: Int = workQueue.size()

    override protected def pollTask: Runnable = workQueue.poll()

    override protected def takeTask: Runnable = workQueue.take()
}