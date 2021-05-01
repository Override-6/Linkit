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

package fr.linkit.engine.local.utils

import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool
import fr.linkit.engine.local.concurrency.pool.BusyWorkerPool.currentTasksId

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class ConsumerContainer[A]() {

    private val consumers = ListBuffer.empty[ConsumerExecutor]

    def add(consumer: A => Unit): this.type = {
        consumers += ConsumerExecutor(consumer, false)
        this
    }

    /**
     * Will add the consumer in the list, and remove it once it was executed.
     *
     * @param consumer the action to perform
     * */
    def addOnce(consumer: A => Unit): this.type = {
        consumers += ConsumerExecutor(consumer, true)
        this
    }

    def remove(consumer: A => Unit): this.type = {
        consumers.filterInPlace(_.isSameConsumer(consumer))
        this
    }

    def clear(): this.type = {
        consumers.clear()
        this
    }

    def foreach(action: (A => Unit) => Unit): Unit = {
        consumers.foreach(p => action(p.consumer))
    }

    /**
     * alias for [[ConsumerContainer#add()]]
     * */
    def +=(consumer: A => Unit): this.type = add(consumer)

    /**
     * alias for [[ConsumerContainer#addOnce()]]
     * */
    def +:+=(consumer: A => Unit): this.type = addOnce(consumer)

    /**
     * alias for [[ConsumerContainer#remove()]]
     * */
    def -=(consumer: A => Unit): this.type = remove(consumer)

    @workerExecution
    def applyAllLater(t: A, onException: Throwable => Unit = _.printStackTrace()): this.type = {
        val pool = BusyWorkerPool.ensureCurrentIsWorker("Async execution is impossible for this consumer container in a non worker execution thread.")
        pool.runLater {
            applyAll(t, onException)
        }
        this
    }

    def applyAll(t: A, onException: Throwable => Unit = _.printStackTrace()): this.type = {
        Array.from(consumers).foreach(consumer => {
            try {
                AppLogger.vTrace(s"EXECUTING $consumer")
                consumer.execute(t)
                AppLogger.vTrace(s"Consumer $consumer executed !")
            } catch {
                case NonFatal(e) =>
                    onException(e)
                    AppLogger.fatal("EXITING VM...")
                    System.exit(1)
            }
        })
        this
    }

    override def toString: String = s"ConsumerContainer($consumers)"

    private case class ConsumerExecutor(consumer: A => Unit, executeOnce: Boolean) {

        def execute(t: A): Unit = {
            if (executeOnce) consumer.synchronized {
                //synchronise in order to be sure that another thread would not start to execute the
                //consumer again when the first thread is removing it from the queue.
                consumer(t)
                remove(consumer)
                return
            }
            consumer(t)
        }

        def isSameConsumer(consumer: A => Unit): Boolean = this.consumer == consumer
    }

}

object ConsumerContainer {

    def apply[T](): ConsumerContainer[T] = new ConsumerContainer()
}
