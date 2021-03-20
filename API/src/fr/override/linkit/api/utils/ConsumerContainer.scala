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

package fr.`override`.linkit.api.utils

import fr.`override`.linkit.api.concurrency.AsyncExecutionContext

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.control.NonFatal

case class ConsumerContainer[T]() {

    private val consumers = ListBuffer.empty[T => Unit]

    def add(consumer: T => Unit): this.type = {
        consumers += consumer
        this
    }

    def remove(consumer: T => Unit): ConsumerContainer.this.type = {
        consumers -= consumer
        this
    }

    /**
     * alias for [[add()]]
     * */
    def +=(consumer: T => Unit): this.type = add(consumer)


    def -=(consumer: T => Unit): this.type = remove(consumer)

    def applyAllAsync(t: T, onException: Throwable => Unit = _.printStackTrace()): this.type = {
        Future {
            applyAll(t, onException)
        }(AsyncExecutionContext)
        this
    }

    def applyAll(t: T, onException: Throwable => Unit = _.printStackTrace()): this.type = {
        consumers.clone().foreach(consumer => {
            try {
                consumer(t)
            } catch {
                case NonFatal(e) => onException(e)
            }
        })
        this
    }

    override def toString: String = s"ConsumerContainer($consumers)"

}
