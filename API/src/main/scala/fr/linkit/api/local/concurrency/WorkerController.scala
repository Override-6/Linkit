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

trait WorkerController {

    @workerExecution
    def pauseTask(): Unit

    @workerExecution
    def pauseTaskWhile(wakeupCondition: => Boolean): Unit

    @workerExecution
    def pauseTaskForAtLeast(millis: Long): Unit

    @workerExecution
    def wakeupNTask(n: Int): Unit

    @workerExecution
    def wakeupAnyTask(): Unit

    @workerExecution
    def wakeupAllTasks(taskIds: Int*): Unit

    @workerExecution
    def wakeupWorkerTask(task: AsyncTask[_]): Unit
}
