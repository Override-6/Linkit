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

package fr.linkit.api.internal.concurrency

import org.jetbrains.annotations.Nullable

import scala.concurrent.Future
import scala.util.Try

/**
 * This class acts as a Future, and contains some utility method for
 * flow control or information about the task that is being executed.
 * */
trait AsyncTask[A] extends Future[A] {
    /**
     * The taskID this class
     * */
    val taskID: Int

    def getWorker: Worker

    /**
     * true if the worker thread is running the task or if the worker is executing other tasks while this one is paused
     * */
    def isExecuting: Boolean

    /**
     * true if the task has been paused
     * */
    def isPaused: Boolean
    
    /**
     * true if the worker is running this task
     * */
    def isRunning: Boolean

    @Nullable val parent: AsyncTask[_]

    def continue(): Unit

    def join(): Try[A]

    def join(millis: Long): Option[Try[A]]

    def throwNextThrowable(): Unit

    def addOnNextThrow(@workerExecution callback: Option[Throwable] => Unit): Unit

    @workerExecution
    def derivate(): Try[A]

    @workerExecution
    def derivateForAtLeast(millis: Long): Option[Try[A]]

}
