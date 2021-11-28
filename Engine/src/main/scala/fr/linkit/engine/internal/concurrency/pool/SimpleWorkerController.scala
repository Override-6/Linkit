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

package fr.linkit.engine.internal.concurrency.pool

import fr.linkit.api.internal.concurrency.{AsyncTask, WorkerController, WorkerPools, WorkerThread, workerExecution}
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.api.internal.concurrency.WorkerPools.{currentTasksId, currentWorker}
import fr.linkit.engine.internal.concurrency.pool.SimpleWorkerController.ControlTicket

import scala.collection.mutable

class SimpleWorkerController extends WorkerController {

    private val workingThreads = new mutable.HashMap[Int, ControlTicket]()

    @workerExecution
    override def pauseTask(): Unit = {
        //unlock condition as false means that we can be unlocked at any time.
        createControlTicket(false)
    }

    @workerExecution
    override def pauseTaskWhile(condition: => Boolean): Unit = {
        if (!condition)
            return
        createControlTicket(condition)
    }

    @workerExecution
    override def pauseTaskForAtLeast(millis: Long): Unit = {
        val worker      = currentWorker
        val currentTask = worker.getCurrentTask.get
        val lockDate    = System.currentTimeMillis()
        workingThreads.put(currentTask.taskID, new ControlTicket(currentTask, System.currentTimeMillis() - lockDate <= millis))
        pauseCurrentTask(millis)
    }

    override def wakeupNTask(n: Int): Unit = {
        val count = workingThreads.size
        val x = if (n > count || n < 0) count else n
        for (_ <- 0 to x) {
            wakeupAnyTask()
        }
    }

    @workerExecution
    override def wakeupAnyTask(): Unit = {
        AppLogger.vError(s"$currentTasksId <> entertainedThreads = " + workingThreads)
        val opt = workingThreads.find(entry => entry._2.isConditionFalse)
        if (opt.isEmpty)
            return

        val entry  = opt.get
        val ticket = entry._2
        val taskID = entry._1

        wakeupWorkerTask(ticket.task)
        workingThreads -= taskID
    }

    @workerExecution
    override def wakeupTasks(taskIds: Seq[Int]): Unit = {
        AppLogger.vError(s"$currentTasksId <> entertainedThreads = " + workingThreads)

        if (workingThreads.isEmpty) {
            AppLogger.vError("THREADS ARE EMPTY !")
            return //Instructions below could throw NoSuchElementException when removing unamused list to entertainedThreads.
        }

        for ((taskID, ticket) <- workingThreads.clone()) {
            if (taskIds.contains(taskID)) {
                wakeupWorkerTask(ticket.task)
                workingThreads -= taskID
            }
        }
    }

    @workerExecution
    override def wakeupWorkerTask(task: AsyncTask[_]): Unit = {
        if (!workingThreads.contains(task.taskID))
            throw new NoSuchElementException(s"Provided thread is not handled by this controller ! (${task.getWorkerThread.getName})")

        task.wakeup()
    }

    def pauseCurrentTask(): Unit = WorkerPools.ensureCurrentIsWorker().pauseCurrentTask()

    def pauseCurrentTask(millis: Long): Unit = WorkerPools.ensureCurrentIsWorker().pauseCurrentTaskForAtLeast(millis)

    private def createControlTicket(pauseCondition: => Boolean): Unit = {
        val currentTask = WorkerPools.currentTask.get
        workingThreads.put(currentTask.taskID, new ControlTicket(currentTask, pauseCondition))
        pauseCurrentTask()
    }

}

object SimpleWorkerController {

    private class ControlTicket(val task: AsyncTask[_], condition: => Boolean) {

        def isConditionFalse: Boolean = !condition

        def getWorker: WorkerThread = task.getWorkerThread
    }

}
