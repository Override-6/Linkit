/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.core.local.utils

import fr.`override`.linkit.api.local.concurrency.workerExecution
import fr.`override`.linkit.core.local.concurrency.BusyWorkerPool

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class ConsumerContainer[T]() {

    private val consumers = ListBuffer.empty[ConsumerExecutor]

    def add(consumer: T => Unit): this.type = {
        consumers += new ConsumerExecutor(consumer, false)
        this
    }

    /**
     * Will add the consumer in the list, and remove it once it was executed.
     *
     * @param consumer the action to perform
     * */
    def addOnce(consumer: T => Unit): this.type = {
        consumers += new ConsumerExecutor(consumer, true)
        this
    }

    def remove(consumer: T => Unit): this.type = {
        consumers.filterInPlace(_.isSameConsumer(consumer))
        this
    }

    def clear(): this.type = {
        consumers.clear()
        this
    }

    /**
     * alias for [[ConsumerContainer#add()]]
     * */
    def +=(consumer: T => Unit): this.type = add(consumer)

    /**
     * alias for [[ConsumerContainer#addOnce()]]
     * */
    def +:+=(consumer: T => Unit): this.type = addOnce(consumer)

    /**
     * alias for [[ConsumerContainer#remove()]]
     * */
    def -=(consumer: T => Unit): this.type = remove(consumer)

    @workerExecution
    def applyAllAsync(t: T, onException: Throwable => Unit = _.printStackTrace()): this.type = {
        BusyWorkerPool.checkCurrentIsWorker("Async execution is impossible for this consumer container in a non worker execution thread.")
        //Will be
        BusyWorkerPool.currentPool()
                .get
                .runLater {
                    applyAll(t, onException)
                }
        this
    }

    def applyAll(t: T, onException: Throwable => Unit = _.printStackTrace()): this.type = {
        consumers.indices.foreach(i => {
            try {
                val consumer = consumers(i)
                consumer.execute(t)
            } catch {
                case NonFatal(e) => onException(e)
            }
        })
        this
    }

    override def toString: String = s"ConsumerContainer($consumers)"

    private class ConsumerExecutor(consumer: T => Unit, executeOnce: Boolean) {
        def execute(t: T): Unit = {
            if (executeOnce) consumer.synchronized {
                //synchronise in order to be sure that another thread would not start to execute the
                //consumer again when the first thread is removing it from the queue.
                consumer(t)
                remove(consumer)
                return
            }
            consumer(t)
        }

        def isSameConsumer(consumer: T => Unit): Boolean = this.consumer == consumer
    }

}

object ConsumerContainer {
    def apply[T](): ConsumerContainer[T] = new ConsumerContainer()
}
