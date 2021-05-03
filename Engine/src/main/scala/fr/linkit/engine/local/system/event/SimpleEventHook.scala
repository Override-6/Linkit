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

package fr.linkit.engine.local.system.event

import fr.linkit.api.local.concurrency.{WorkerPools, workerExecution}
import fr.linkit.api.local.system.event.{Event, EventHook, EventListener}
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool
import fr.linkit.engine.local.utils.ConsumerContainer

class SimpleEventHook[L <: EventListener, E <: Event[_, L]](listenerMethods: ((L, E) => Unit)*) extends EventHook[L, E] {

    private val consumers = ConsumerContainer[E]()

    @workerExecution
    override def await(): Unit = {
        val pool = WorkerPools.ensureCurrentIsWorker()
        val task = WorkerPools.currentTask
        addOnce {
            task.wakeup()
        }
        pool.pauseCurrentTask()
    }

    override def add(action: E => Unit): Unit = consumers += action

    override def addOnce(action: E => Unit): Unit = consumers +:+= action

    override def add(action: => Unit): Unit = add(_ => action)

    override def addOnce(action: => Unit): Unit = addOnce(_ => action)

    override def executeEvent(event: E, listeners: Seq[L]): Unit = {
        listeners.foreach(listener => listenerMethods.foreach(_ (listener, event)))
        consumers.applyAll(event)
    }
}

object SimpleEventHook {

    def apply[L <: EventListener, E <: Event[_, L]](methods: (L, E) => Unit*): SimpleEventHook[L, E] = {
        new SimpleEventHook[L, E](methods: _*)
    }
}
