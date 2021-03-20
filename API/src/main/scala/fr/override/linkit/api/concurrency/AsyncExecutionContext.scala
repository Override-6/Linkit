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

package fr.`override`.linkit.api.concurrency

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object AsyncExecutionContext extends ExecutionContext {
    private val ses = Executors.newFixedThreadPool(5, (r: Runnable) => {
        println("Created new thread for async context !")
        new Thread(SyncExecutionContext.threadGroup, r)
    })

    override def execute(runnable: Runnable): Unit = ses.submit(runnable)

    override def reportFailure(cause: Throwable): Unit = cause.printStackTrace()
}
