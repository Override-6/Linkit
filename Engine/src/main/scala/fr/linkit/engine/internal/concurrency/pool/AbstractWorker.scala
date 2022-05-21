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

import fr.linkit.api.internal.concurrency._
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.internal.concurrency.SimpleAsyncTask
import fr.linkit.engine.internal.concurrency.pool.AbstractWorker.TaskProfile

import java.util.concurrent.locks.LockSupport
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Try

/**
 * The representation of a java thread, extending from [[Thread]].
 * This class contains information that need to be stored into a specific thread class.
 * */
private[concurrency] trait AbstractWorker
        extends Worker with WorkerThreadController {
    
    private var isParkingForWorkflow   : Boolean    = false
    private var taskRecursionDepthCount: Int        = 0
    private var currentTask            : ThreadTask = _
    private val workingTasks                        = new mutable.LinkedHashMap[Int, TaskProfile]
    private val forcedTasks                         = ListBuffer.empty[ThreadTask]
    override val pool: WorkerPool
    
    override def getCurrentTask: Option[ThreadTask] = Option(currentTask)
    
    override def execWhileCurrentTaskPaused[T](parkAction: => T, loopCondition: => Boolean)(workflow: T => Unit): Unit = {
        ensureCurrentThreadEqualsThis()
        //AppLogger.vError("Entering workflow loop...")
        
        while (loopCondition) {
            //AppLogger.vError("This thread is about to park.")
            isParkingForWorkflow = true
            val t = parkAction
            isParkingForWorkflow = false
            //AppLogger.vError("This thread has been unparked.")
            forcedTasks.foreach(runTask)
            if (!loopCondition) {
                return
            }
            //AppLogger.vError("Continuing workflow...")
            workflow(t)
        }
        //AppLogger.vError("Exiting workflow loop...")
    }
    
    override def runTask(task: ThreadTask): Unit = {
        ensureCurrentThreadEqualsThis()
        
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
        val blocker = LockSupport.getBlocker(thread)
        AppLogger.debug(s"waking up task ${task.taskID}")
        if (blocker == task) {
            LockSupport.unpark(thread)
            task.setContinue()
        } else if (task.taskID != -1) { //-1 task identifier is for mocked tasks
            AppLogger.error(s"Could not wakeup task ${task.taskID}. ($blocker, $this)")
        }
    }
    
    private def pushTask(task: ThreadTask): Unit = {
        workingTasks.put(task.taskID, TaskProfile(task))
        currentTask = task
    }
    
    private def removeTask(task: AsyncTask[_]): Unit = {
        val id = task.taskID
        workingTasks.remove(id)
        currentTask = workingTasks
                .lastOption
                .map(_._2.task)
                .orNull
    }
    
    override def runWhileSleeping(task: => Unit): Unit = {
        if (!isSleeping)
            throw new IllegalThreadStateException("Thread isn't sleeping.")
        val id = if (currentTask == null) 0 else currentTask.taskID + 1
        forcedTasks += new SimpleAsyncTask(id, currentTask, () => Try(task))
        if (currentTask != null)
            wakeup(currentTask)
    }
    
    override def taskRecursionDepth: Int = taskRecursionDepthCount
    
    override def getController: WorkerThreadController = this
    
    override def isSleeping: Boolean = isParkingForWorkflow
    
    @inline
    private def ensureCurrentThreadEqualsThis(): Unit = {
        if (Thread.currentThread() != thread)
            throw IllegalThreadException(s"method not called by thread $thread")
    }
    
}

object AbstractWorker {
    
    case class TaskProfile(task: AsyncTask[_] with AsyncTaskController) {
        
        val taskID: Int = task.taskID
        
    }
    
}
