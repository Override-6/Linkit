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

package fr.linkit.engine.internal.util

import fr.linkit.api.gnom.persistence.context.Deconstructible
import fr.linkit.api.gnom.persistence.context.Deconstructible.Persist
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.engine.internal.concurrency.IllegalThreadException

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class ConsumerContainer[A]@Persist()() extends Deconstructible {

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

    def isEmpty: Boolean = consumers.isEmpty

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
    def +!+=(consumer: A => Unit): this.type = addOnce(consumer)

    def applyAll(t: A, onException: Throwable => Unit = throw _): this.type = {
        if (consumers.isEmpty)
            return this
        Array.from(consumers).foreach(consumer => {
            try {
                consumer.execute(t)
            } catch {
                case NonFatal(e) =>
                    onException(e)
            }
        })
        this
    }

    override def toString: String = s"ConsumerContainer($consumers)"

    private case class ConsumerExecutor(consumer: A => Unit, executeOnce: Boolean) {

        def execute(t: A): Unit = {
            if (executeOnce) consumer.synchronized {
                //synchronize in order to be sure that another thread would not start to execute the
                //consumer again when the first thread is removing it from the queue.
                consumer(t)
                consumers -= this
                return
            }
            consumer(t)
        }

    }

    override def deconstruct(): Array[Any] = Array()
}

object ConsumerContainer {

    def apply[T](): ConsumerContainer[T] = new ConsumerContainer()
}
