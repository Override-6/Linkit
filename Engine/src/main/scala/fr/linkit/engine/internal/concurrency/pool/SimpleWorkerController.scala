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

import fr.linkit.api.internal.concurrency.{WorkerTask, WorkerController, workerExecution}
import fr.linkit.engine.internal.concurrency.pool.SimpleWorkerController.ControlTicket

import scala.collection.mutable

class SimpleWorkerController extends WorkerController {
    
    private val pausedTasks = new mutable.HashMap[Int, ControlTicket]()
    
    protected def tickets: mutable.HashMap[Int, ControlTicket] = {
        if (pausedTasks.nonEmpty) {
            pausedTasks.filterInPlace((_, tick) => tick.task.isExecuting)
            return pausedTasks.filter(_._2.task.isPaused)
        }
        pausedTasks
    }
    
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
        val lockDate = System.currentTimeMillis()
        createControlTicket(System.currentTimeMillis() - lockDate <= millis)
        pauseCurrentTask(millis)
    }
    
    override def wakeupNTask(n: Int): Unit = this.synchronized {
        val count = pausedTasks.size
        val x     = if (n > count || n < 0) count else n
        for (_ <- 0 to x) {
            wakeupAnyTask()
        }
    }
    
    override def toString: String = pausedTasks.mkString("SimpleWorkerController(", ",", ")")
    
    @workerExecution
    override def wakeupAnyTask(): Unit = this.synchronized {
        val opt = tickets.find(entry => entry._2.shouldWakeup)
        //AppLogger.debug(s"wakeupAnyTask $this (${System.identityHashCode(this)})")
        if (opt.isEmpty) {
            return
        }
        
        val entry  = opt.get
        val ticket = entry._2
        
        wakeupWorkerTask(ticket.task)
    }
    
    @workerExecution
    override def wakeupTasks(taskIds: Seq[Int]): Unit = this.synchronized {
        if (pausedTasks.isEmpty)
            return
        
        for ((taskID, ticket) <- pausedTasks.clone()) {
            if (taskIds.contains(taskID)) {
                wakeupWorkerTask(ticket.task)
            }
        }
    }
    
    @workerExecution
    override def wakeupWorkerTask(task: WorkerTask[_]): Unit = this.synchronized {
        val taskID = task.taskID
        if ((pausedTasks remove taskID).isEmpty)
            throw new NoSuchElementException(s"Provided thread is not handled by this controller ! (${task.getWorker.thread.getName})")
        if (task.isPaused)
            task.continue()
    }
    

    private def pauseCurrentTask(millis: Long): Unit = EngineWorkerPools.ensureCurrentIsWorker().pauseCurrentTaskForAtLeast(millis)
    
    protected def createControlTicket(pauseCondition: => Boolean): Unit = {
        this.synchronized {
            val currentTask = EngineWorkerPools.currentTask.get
            pausedTasks.put(currentTask.taskID, new ControlTicket(currentTask, pauseCondition))
        }
        EngineWorkerPools.ensureCurrentIsWorker().pauseCurrentTask()
    }
    
}

object SimpleWorkerController {
    
    protected class ControlTicket(val task: WorkerTask[_], condition: => Boolean) {
        
        def shouldWakeup: Boolean = !condition
        
        override def toString: String = s"task: $task"
        
    }
    
}
