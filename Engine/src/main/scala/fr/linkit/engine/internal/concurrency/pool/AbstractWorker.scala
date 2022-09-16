/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.concurrency.pool

import fr.linkit.api.internal.concurrency.{AsyncTask, AsyncTaskController, IllegalThreadException, InternalWorkerThread, Worker, _}
import fr.linkit.api.internal.concurrency.pool.WorkerPool
import fr.linkit.api.internal.system.log.AppLoggers
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
        extends Worker with InternalWorkerThread {
    
    private var isParkingForWorkflow   : Boolean    = false
    private var taskRecursionDepthCount: Int        = 0
    private var currentTask            : ThreadTask = _
    private val workingTasks                        = new mutable.LinkedHashMap[Int, TaskProfile]
    private val forcedTasks                         = ListBuffer.empty[ThreadTask]
    override val pool: WorkerPool
    
    private var currentLock = new Object
    
    protected def nextPoolTaskCount: Int
    
    override def getCurrentTask: Option[ThreadTask] = Option(currentTask)
    
    override def getTaskStack: Array[Int] = workingTasks.keys.toArray
    
    override def execWhileCurrentTaskPaused[T](parkAction: => T, loopCondition: => Boolean)(workflow: T => Unit): Unit = {
        ensureCurrentThreadEqualsWorker()
        AppLoggers.Worker.trace("Entering workflow loop...")
        
        while (loopCondition) {
            currentLock.synchronized {
                //if (currentTask != null && !currentTask.isPaused) return
                if (!loopCondition) return
                AppLoggers.Worker.trace("This thread is about to park.")
                isParkingForWorkflow = true
            }
            val t = parkAction
            currentLock.synchronized {
                isParkingForWorkflow = false
                AppLoggers.Worker.trace("This thread has been unparked.")
            }
            val noForcedTasks = executeForcedTasks()
            currentLock.synchronized {
                if (noForcedTasks && !loopCondition) {
                    return
                }
                AppLoggers.Worker.trace("Continuing workflow...")
            }
            workflow(t)
        }
        AppLoggers.Worker.trace("Exiting workflow loop...")
    }
    
    private def executeForcedTasks(): Boolean = {
        if (forcedTasks.isEmpty) return true
        AppLoggers.Worker.trace(s"Executing ${forcedTasks.size} forced tasks.")
        val clone = forcedTasks.clone()
        forcedTasks.clear()
        clone.foreach(runTask)
        false
    }
    
    override def runTask(task: ThreadTask): Unit = {
        ensureCurrentThreadEqualsWorker()
        
        pushTask(task)
        AppLoggers.Worker.trace(s"Task ${task.taskID} started.")
        task.runTask()
        AppLoggers.Worker.trace(s"Task ${task.taskID} ended.")
        removeTask(task)
    }
    
    override def runSubTask(task: Runnable): Unit = {
        taskRecursionDepthCount += 1
        task.run()
        taskRecursionDepthCount -= 1
    }
    
    //use ThreadTask#continue instead of directly call this method.
    override def wakeup(task: ThreadTask): Unit = {
        val blocker = LockSupport.getBlocker(thread)
        AppLoggers.Worker.debug(s"waking up task ${task.taskID}")
        if (blocker == null || (blocker eq task)) {
            LockSupport.unpark(thread)
        } else if (task.taskID != -1) { //-1 task identifier is for mocked tasks
            AppLoggers.Worker.error(s"Could not wakeup task ${task.taskID}. ($blocker, $this)")
        }
    }
    
    private def pushTask(task: ThreadTask): Unit = {
        workingTasks.put(task.taskID, TaskProfile(task))
        currentTask = task
        currentLock = currentTask
    }
    
    private def removeTask(task: AsyncTask[_]): Unit = {
        val id = task.taskID
        workingTasks.remove(id)
        currentTask = workingTasks
                .lastOption
                .map(_._2.task)
                .orNull
        if (currentTask != null) currentLock = currentTask else currentLock = new Object()
    }
    
    override def runWhileSleeping(task: => Unit): Unit = {
        if (!isSleeping)
            throw new IllegalThreadStateException("Thread isn't sleeping.")
        currentLock.synchronized {
            forcedTasks += new SimpleAsyncTask(nextPoolTaskCount, currentTask, () => Try(task))
        }
        if (currentTask != null)
            wakeup(currentTask)
    }
    
    override def taskRecursionDepth: Int = taskRecursionDepthCount
    
    override def getController: InternalWorkerThread = this
    
    override def isSleeping: Boolean = isParkingForWorkflow
    
    @inline
    private def ensureCurrentThreadEqualsWorker(): Unit = {
        if (Thread.currentThread() != thread)
            throw IllegalThreadException(s"method not called by thread $thread")
    }
    
}

object AbstractWorker {
    
    case class TaskProfile(task: AsyncTask[_] with AsyncTaskController) {
        
        val taskID: Int = task.taskID
        
    }
    
}
