package fr.linkit.core.local.concurrency.pool

import fr.linkit.api.local.concurrency.EntertainedThread
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.workerThreadGroup

import java.util.concurrent.locks.LockSupport
import scala.collection.mutable

/**
 * The representation of a java thread, extending from [[Thread]].
 * This class contains information that need to be stored into a specific thread class.
 * */
private[concurrency] final class BusyWorkerThread private[concurrency](target: Runnable,
                                                                       override val owner: BusyWorkerPool,
                                                                       id: Int)
        extends Thread(workerThreadGroup, target, s"${owner.name}'s Thread#$id") with EntertainedThread {

    private[concurrency] var isParkingForWorkflow   : Boolean                   = false
    private[concurrency] var taskRecursionDepthCount: Int                       = 0
    private              var currentTaskID          : Int                       = -1
    private val workflowContinueLevels              : mutable.Map[Int, Boolean] = mutable.HashMap()

    private[concurrency] def workflowLoop[T](parkAction: => T)(workflow: T => Unit): Unit = {
        AppLogger.error(s"$tasksId <> Entering Workflow Loop...")
        while (workflowContinueLevels(currentTaskID)) {
            AppLogger.error(s"$tasksId <> Workflow Loop continuing...")
            isParkingForWorkflow = true
            AppLogger.error(s"$tasksId <> Parking...")
            val t = parkAction
            AppLogger.error(s"$tasksId <> This thread has been unparked.")
            isParkingForWorkflow = false

            if (!workflowContinueLevels(currentTaskID)) {
                AppLogger.error("Workflow returned...")
                workflowContinueLevels(currentTaskID) = true
                return
            }

            AppLogger.error(s"$tasksId <> Continue workflow...")
            workflow(t)
            AppLogger.error(s"$tasksId <> Workflow have ended !")
        }
        this.workflowContinueLevels(currentTaskID) = true //set it to true in case if stopWorkflowLoop has been called.
        AppLogger.error(s"$tasksId <> Exit Worker Loop")
    }

    private[concurrency] def stopWorkflowLoop(taskID: Int): Unit = {
        val continueWorkflow = workflowContinueLevels.get(taskID)
        AppLogger.error(s"stopWorkflowLoop($taskID) called for thread $getName.")
        if (continueWorkflow.isEmpty)
            return
        if (continueWorkflow.get)
            this.workflowContinueLevels(taskID) = false

        AppLogger.error(s"$getName <-- This thread will be unparked for task $taskID because stopWorkflowLoop has been invoked.")
        AppLogger.error(s"workflowContinueLevels = $workflowContinueLevels")
        if (currentTaskID == taskID)
            LockSupport.unpark(this)
    }

    private[concurrency] def pushTaskID(id: Int): Unit = {
        workflowContinueLevels.put(id, true)
        currentTaskID = id
        isUpdated = true
    }

    private[concurrency] def removeTaskID(id: Int): Unit = {
        workflowContinueLevels.remove(id)
        currentTaskID = workflowContinueLevels
                .keys
                .lastOption
                .getOrElse(-1)
        isUpdated = true
    }

    def isExecutingTask(taskID: Int): Boolean = workflowContinueLevels.contains(taskID)

    def getCurrentTaskID: Int = currentTaskID

    def currentTaskIsWaiting(): Boolean = workflowContinueLevels(currentTaskID)

    //debug only
    private var isUpdated          = true
    private var tasksIdStr: String = _

    def tasksId: String = {
        if (workflowContinueLevels.isEmpty)
            return s"[]"
        if (!isUpdated)
            return tasksIdStr

        val current = getCurrentTaskID
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
                .append(']')
                .toString()
        tasksIdStr
    }

    override def isWaitingForRecursiveTask: Boolean = isParkingForWorkflow

    override def taskRecursionDepth: Int = taskRecursionDepthCount

}
