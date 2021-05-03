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

import fr.linkit.api.local.concurrency.{AsyncTask, IllegalThreadException, WorkerThread, WorkerThreadController}
import fr.linkit.api.local.concurrency.WorkerPools.workerThreadGroup
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.locks.LockSupport

/**
 * The representation of a java thread, extending from [[Thread]].
 * This class contains information that need to be stored into a specific thread class.
 * */
private[concurrency] final class BusyWorkerThread private[concurrency](target: Runnable,
                                                                       override val pool: BusyWorkerPool,
                                                                       tid: Int)
    extends Thread(workerThreadGroup, target, s"${pool.name}'s Thread#$tid") with WorkerThread with WorkerThreadController {

    private var isParkingForWorkflow   : Boolean    = false
    private var taskRecursionDepthCount: Int        = 0
    private var currentTask            : ThreadTask = _
    private val workingTasks                        = new LinkedBlockingDeque[ThreadTask]()

    override def getCurrentTask: Option[ThreadTask] = Option(currentTask)

    def isExecutingTask(taskID: Int): Boolean = {
        workingTasks
            .stream()
            .anyMatch(_.taskID == taskID)
    }

    override def execWhileCurrentTaskPaused[T](parkAction: => T)(workflow: T => Unit): Unit = {
        checkCurrentThreadEqualsCurrentObject()

        while (!currentTask.isWaitingToContinue) {
            isParkingForWorkflow = true
            val t = parkAction
            isParkingForWorkflow = false
            if (currentTask.isWaitingToContinue) {
                return
            }

            workflow(t)
        }
        //this.workflowContinueLevels(getCurrentTaskID) = true //set it to true in case if stopWorkflowLoop has been called.
    }

    override def runTask(task: ThreadTask): Unit = {
        if (Thread.currentThread() != this)
            throw IllegalThreadException("")

        pushTask(task)
        task.runTask()
        removeTask(task)
    }

    override def runSubTask(task: Runnable): Unit = {
        taskRecursionDepthCount += 1
        task.run()
        taskRecursionDepthCount -= 1
    }

    override def wakeup(): Unit = if (isParkingForWorkflow) LockSupport.unpark(this)

    private def pushTask(task: ThreadTask): Unit = {
        workingTasks.addLast(task)
        currentTask = task
        tasksIdStr = getUpdatedTasksID
    }

    private def removeTask(task: AsyncTask[_]): Unit = {
        val id = task.taskID
        currentTask = workingTasks.pollLast()
        tasksIdStr = getUpdatedTasksID
    }

    //debug only
    private var tasksIdStr: String = _

    private def getUpdatedTasksID: String = {
        if (workingTasks.isEmpty)
            return s"[]"

        val sb = new StringBuilder(s"[")
        workingTasks.forEach(task => {
            val taskID = task.taskID
            if (taskID == getCurrentTaskID) {
                sb.append(taskID)
            } else {
                sb.append(taskID)
            }
            sb.append(',')
        })
        tasksIdStr = sb.dropRight(1) // remove last ',' char
            .append("](")
            .append(currentTask)
            .append(")")
            .toString()
        tasksIdStr
    }

    override def prettyPrintPrefix: String = tasksIdStr

    override def taskRecursionDepth: Int = taskRecursionDepthCount

    override def getController: WorkerThreadController = this

    override def isSleeping: Boolean = isParkingForWorkflow

    private def checkCurrentThreadEqualsCurrentObject(): Unit = {
        if (Thread.currentThread() != this)
            throw IllegalThreadException("This Thread must run methods of it's own object representation.")
    }
}
