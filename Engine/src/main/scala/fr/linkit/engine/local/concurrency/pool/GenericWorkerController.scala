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

package fr.linkit.engine.local.concurrency.pool

class GenericWorkerController extends AbstractWorkerController[BusyWorkerThread] {

    override def currentWorker: BusyWorkerThread = BusyWorkerPool.currentWorker

    override def notifyWorker(worker: BusyWorkerThread, taskID: Int): Unit = BusyWorkerPool.unpauseTask(worker, taskID)

    override def pauseCurrentTask(): Unit = {
        currentWorker.pool.pauseCurrentTask()
    }

    override def pauseCurrentTask(millis: Long): Unit = {
        currentWorker.pool.pauseCurrentTaskForAtLeast(millis)
    }
}
