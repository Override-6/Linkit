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

import scala.concurrent.Future
import scala.util.Try

trait AsyncTaskFuture[A] extends Future[A] {

    val taskID: Int

    def join(): Try[A]

    def join(millis: Long): Option[Try[A]]

    @workerExecution
    def joinTask(): Try[A]

    @workerExecution
    def joinTaskForAtLeast(millis: Long): Option[Try[A]]

}
