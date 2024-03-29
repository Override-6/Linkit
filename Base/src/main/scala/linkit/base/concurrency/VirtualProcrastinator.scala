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

package linkit.base.concurrency

import fr.linkit.api.internal.concurrency.{Procrastinator, Worker, WorkerPool}
import fr.linkit.api.internal.system.log.AppLoggers
import VirtualProcrastinator.workers

import java.util.concurrent.{Executors, Future}
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import scala.util.control.NonFatal

class VirtualProcrastinator(val name: String) extends WorkerPool {

    private val taskCounter = new AtomicInteger()
    private val executor    = {
        Executors.newThreadPerTaskExecutor(makeVThread(_))
    }

    override def runLater[A](f: => A): Future[A] = {
        AppLoggers.Debug.trace("in Run Later")
        executor.submit(() => {
            AppLoggers.Debug.trace("Executing task...")
            val v = try f catch {
                case t: Throwable =>
                    t.printStackTrace()
                    throw t
            }
            AppLoggers.Debug.trace("Task ended.")
            v
        })
    }

    private def makeVThread(target: Runnable): Thread = try {
        AppLoggers.Debug.trace("Making vThread...")
        val taskID = taskCounter.incrementAndGet()
        val thread = Thread.ofVirtual()
                .name(name + "'s task #" + taskID)
                .unstarted(target)
        val worker = new VirtualWorker(this, thread, taskID)
        workers.put(thread, worker)
        thread
    } catch {
        case e: Throwable =>
            e.printStackTrace()
            null
    }
}

object VirtualProcrastinator extends Procrastinator.Supplier {


    override def currentTaskId: Int = currentWorker.map(_.taskID).getOrElse(-1)

    private val workers = mutable.HashMap.empty[Thread, VirtualWorker]

    override def apply(name: String): VirtualProcrastinator = new VirtualProcrastinator(name)

    override def current: Option[Procrastinator] = workers.get(Thread.currentThread()).map(_.pool)

    override def currentWorker: Option[Worker] = workers.get(Thread.currentThread())
}
