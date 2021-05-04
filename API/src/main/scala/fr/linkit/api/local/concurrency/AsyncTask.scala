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

package fr.linkit.api.local.concurrency

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

    def getWorkerThread: WorkerThread

    def isExecuting: Boolean

    def isPaused: Boolean

    @Nullable val parent: AsyncTask[_]

    def wakeup(): Unit

    def join(): Try[A]

    def join(millis: Long): Option[Try[A]]

    def throwNextThrowable(): Unit

    def addOnNextThrow(@workerExecution callback: Option[Throwable] => Unit): Unit

    @workerExecution
    def derivate(): Try[A]

    @workerExecution
    def derivateForAtLeast(millis: Long): Option[Try[A]]

}
