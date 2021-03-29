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

package fr.linkit.core.local.concurrency

import fr.linkit.api.local.system.AppLogger

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

object SyncExecutionContext extends ExecutionContext {

    val threadGroup = new ThreadGroup("Relay Execution Context")

    private val queue: BlockingDeque[Runnable] = new LinkedBlockingDeque()

    private val worker = new Thread(threadGroup, () => {
        while (true) try {
            val action = queue.takeLast()
            action.run()
        } catch {
            case NonFatal(e) => AppLogger.printStackTrace(e)
        }
    })

    worker.setName("Relay Async Execution Worker")
    worker.start()

    override def execute(runnable: Runnable): Unit = queue.addFirst(runnable)

    override def reportFailure(cause: Throwable): Unit = AppLogger.printStackTrace(cause)
}
