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

package fr.linkit.api.local.concurrency

trait WorkerController[W <: WorkerThread] {

    @workerExecution
    def waitTask(): Unit

    @workerExecution
    def waitTaskWhile(notifyCondition: => Boolean): Unit

    @workerExecution
    def waitTaskForAtLeast(millis: Long): Unit

    @workerExecution
    def notifyNThreads(n: Int): Unit

    @workerExecution
    def notifyAnyThread(): Unit

    @workerExecution
    def notifyThreadsTasks(taskIds: Int*): Unit

    @workerExecution
    def notifyWorkerTask(thread: W, taskID: Int): Unit
}
