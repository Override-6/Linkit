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

import fr.linkit.api.internal.concurrency.WorkerPools.workerThreadGroup
import fr.linkit.api.internal.concurrency._
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.internal.concurrency.pool.BusyWorkerThread.TaskProfile
import java.util.concurrent.locks.LockSupport

import scala.collection.mutable

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
    private val workingTasks                        = new mutable.LinkedHashMap[Int, TaskProfile]

    override def getCurrentTask: Option[ThreadTask] = Option(currentTask)

    def isExecutingTask(taskID: Int): Boolean = {
        workingTasks.contains(taskID)
    }

    override def execWhileCurrentTaskPaused[T](parkAction: => T, loopCondition: => Boolean)(workflow: T => Unit): Unit = {
        ensureCurrentThreadEqualsThisObject()
        AppLogger.vError("Entering workflow loop...")

        while (loopCondition) {
            AppLogger.vError("This thread is about to park.")
            isParkingForWorkflow = true
            val t = parkAction
            isParkingForWorkflow = false
            AppLogger.vError("This thread has been unparked.")
            if (!loopCondition) {
                return
            }
            AppLogger.vError("Continuing workflow...")
            workflow(t)
        }
        AppLogger.vError("Exiting workflow loop...")
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

    override def wakeup(task: ThreadTask): Unit = {
        AppLogger.vDebug(s"Waking up thread $this for task ${task.taskID}")
        val blocker = LockSupport.getBlocker(this)
        AppLogger.vDebug(s"Thread $this is parking on blocker $blocker")
        if (blocker == task) {
            AppLogger.vError(s"$this <- This thread will be unparked.")
            LockSupport.unpark(this)
            task.setContinue()
        }
    }

    private def pushTask(task: ThreadTask): Unit = {
        workingTasks.put(task.taskID, TaskProfile(task))
        currentTask = task
        tasksIdStr = getUpdatedTasksID
        AppLogger.vInfo(s"$tasksIdStr push Task '${task.taskID}'")
    }

    private def removeTask(task: AsyncTask[_]): Unit = {
        val id = task.taskID
        workingTasks.remove(id)
        currentTask = workingTasks
                .lastOption
                .map(_._2.task)
                .orNull
        tasksIdStr = getUpdatedTasksID
        AppLogger.vInfo(s"$tasksIdStr remove Task '$id'")
    }

    //debug only
    private var tasksIdStr: String = _

    private def getUpdatedTasksID: String = {
        if (workingTasks.isEmpty)
            return s"[]"

        val sb = new StringBuilder(s"[")
        workingTasks.values.foreach(taskProfile => {
            val taskID = taskProfile.taskID
            if (taskID == getCurrentTaskID) {
                sb.append(taskID)
            } else {
                sb.append(taskID)
            }
            sb.append(',')
        })
        tasksIdStr = sb.dropRight(1) // remove last ',' char
                .append("](")
                .append(currentTask.taskID)
                .append(")")
                .toString()
        tasksIdStr
    }

    override def prettyPrintPrefix: String = tasksIdStr

    override def taskRecursionDepth: Int = taskRecursionDepthCount

    override def getController: WorkerThreadController = this

    override def isSleeping: Boolean = isParkingForWorkflow

    private def ensureCurrentThreadEqualsThisObject(): Unit = {
        if (Thread.currentThread() != this)
            throw IllegalThreadException("This Thread must run methods of it's own object representation.")
    }

}

object BusyWorkerThread {

    case class TaskProfile(task: AsyncTask[_] with AsyncTaskController) {

        val taskID: Int = task.taskID

        var mustWakeup: Boolean = true

    }

}
