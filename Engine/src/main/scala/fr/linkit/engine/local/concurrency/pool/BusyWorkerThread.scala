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

import fr.linkit.api.local.concurrency.WorkerThread
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool.workerThreadGroup

import java.util.concurrent.locks.LockSupport
import scala.collection.mutable

/**
 * The representation of a java thread, extending from [[Thread]].
 * This class contains information that need to be stored into a specific thread class.
 * */
private[concurrency] final class BusyWorkerThread private[concurrency](target: Runnable,
                                                                       override val pool: BusyWorkerPool,
                                                                       id: Int)
        extends Thread(workerThreadGroup, target, s"${pool.name}'s Thread#$id") with WorkerThread {

    private[concurrency] var isParkingForWorkflow   : Boolean                   = false
    private[concurrency] var taskRecursionDepthCount: Int                       = 0
    private              var currentTaskId          : Int                       = -1
    private val workflowContinueLevels              : mutable.Map[Int, Boolean] = mutable.LinkedHashMap()

    override def currentTaskID: Int = currentTaskId

    def isExecutingTask(taskID: Int): Boolean = workflowContinueLevels.contains(taskID)

    def currentTaskIsWaiting(): Boolean = workflowContinueLevels.getOrElse(currentTaskID, false)

    private[concurrency] def workflowLoop[T](parkAction: => T)(workflow: T => Unit): Unit = {
        AppLogger.vError(s"$tasksId <> Entering Workflow Loop... ")
        while (workflowContinueLevels(currentTaskID)) {
            AppLogger.vError(s"$tasksId <> Workflow Loop continuing... ")
            isParkingForWorkflow = true
            AppLogger.vError(s"$tasksId <> Parking... ")
            val t = parkAction
            AppLogger.vError(s"$tasksId <> This thread has been unparked. ")
            isParkingForWorkflow = false

            if (!workflowContinueLevels(currentTaskID)) {
                AppLogger.vError(s"Workflow returned... ")
                workflowContinueLevels(currentTaskID) = true
                return
            }

            AppLogger.vError(s"$tasksId <> Continue workflow... ")
            workflow(t)
            AppLogger.vError(s"$tasksId <> Workflow have ended ! ")
        }
        this.workflowContinueLevels(currentTaskID) = true //set it to true in case if stopWorkflowLoop has been called.
        AppLogger.vError(s"$tasksId <> Exit Worker Loop ")
    }

    private[concurrency] def stopWorkflowLoop(taskID: Int): Unit = {
        val continueWorkflow = workflowContinueLevels.get(taskID)
        AppLogger.vError(s"stopWorkflowLoop($taskID) called for thread $getName.")
        if (continueWorkflow.isEmpty)
            return
        if (continueWorkflow.get)
            this.workflowContinueLevels(taskID) = false

        AppLogger.vError(s"$getName <-- This thread will be unparked for task $taskID because stopWorkflowLoop has been invoked.")
        AppLogger.vError(s"workflowContinueLevels = $workflowContinueLevels")
        if (currentTaskID == taskID)
            LockSupport.unpark(this)
    }


    private[concurrency] def pushTaskID(id: Int): Unit = {
        workflowContinueLevels.put(id, true)
        currentTaskId = id
        tasksIdStr = getUpdatedTasksID
    }

    private[concurrency] def removeTaskID(id: Int): Unit = {
        workflowContinueLevels.remove(id)
        currentTaskId = workflowContinueLevels
                .keys
                .lastOption
                .getOrElse(-1)
        tasksIdStr = getUpdatedTasksID
    }



    //debug only
    private var tasksIdStr: String = _

    private def getUpdatedTasksID: String = {
        if (workflowContinueLevels.isEmpty)
            return s"[]"

        val current = currentTaskId
        val sb      = new StringBuilder(s"[")
        workflowContinueLevels.keys.foreach(taskID => {
            if (taskID == current) {
                sb.append(taskID)
            } else {
                sb.append(taskID)
            }
            sb.append(',')
        })
        tasksIdStr = sb.dropRight(1) // remove last ',' char
                .append("](")
                .append(currentTaskID)
                .append(")")
                .toString()
        tasksIdStr
    }

    def tasksId: String = tasksIdStr

    override def isParkingForRecursiveTask: Boolean = isParkingForWorkflow

    override def taskRecursionDepth: Int = taskRecursionDepthCount

}
