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

import fr.linkit.api.local.concurrency.{WorkerController, WorkerThread, workerExecution}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.local.concurrency.pool.AbstractWorkerController.ControlTicket
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool.currentTasksId

import scala.collection.mutable

abstract class AbstractWorkerController[W <: WorkerThread] extends WorkerController[W] {

    private val entertainedThreads = new mutable.HashMap[Int, ControlTicket[W]]()

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
        val currentTask = worker.currentTaskID
        val lockDate    = System.currentTimeMillis()
        entertainedThreads.put(currentTask, new ControlTicket[W](worker, System.currentTimeMillis() - lockDate <= millis))
        pauseCurrentTask(millis)
    }

    override def notifyNThreads(n: Int): Unit = {
        for (_ <- 0 to n) {
            notifyAnyThread()
        }
    }

    @workerExecution
    override def notifyAnyThread(): Unit = {
        AppLogger.vError(s"$currentTasksId <> entertainedThreads = " + entertainedThreads)
        val opt = entertainedThreads.find(entry => entry._2.canBeNotified)
        if (opt.isEmpty)
            return

        val entry  = opt.get
        val ticket = entry._2
        val taskID = entry._1

        notifyWorkerTask(ticket.getWorker, taskID)
        entertainedThreads -= taskID
    }

    @workerExecution
    override def notifyThreadsTasks(taskIds: Int*): Unit = {
        AppLogger.vError(s"$currentTasksId <> entertainedThreads = " + entertainedThreads)

        if (entertainedThreads.isEmpty) {
            AppLogger.vError("THREADS ARE EMPTY !")
            return //Instructions below could throw NoSuchElementException when removing unamused list to entertainedThreads.
        }

        for ((taskID, ticket) <- entertainedThreads.clone()) {
            if (taskIds.contains(taskID)) {
                notifyWorkerTask(ticket.getWorker, taskID)
                entertainedThreads -= taskID
            }
        }
    }

    @workerExecution
    override def notifyWorkerTask(thread: W, taskID: Int): Unit = {
        if (!entertainedThreads.contains(taskID))
            throw new NoSuchElementException(s"Provided thread is not entertained by this entertainer ! (${thread.getName})")
        notifyWorker(thread, taskID)
    }

    def currentWorker: W

    def notifyWorker(worker: W, taskID: Int): Unit

    def pauseCurrentTask(): Unit

    def pauseCurrentTask(millis: Long): Unit

    private def createControlTicket(pauseCondition: => Boolean): Unit = {
        val worker      = currentWorker
        val currentTask = worker.currentTaskID
        entertainedThreads.put(currentTask, new ControlTicket[W](worker, pauseCondition))
        pauseCurrentTask()
    }

}

object AbstractWorkerController {

    private class ControlTicket[W <: WorkerThread](worker: W, condition: => Boolean) {
        def canBeNotified: Boolean = !condition

        def getWorker: W = worker
    }

}
