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

package fr.linkit.engine.internal.concurrency.pool

import fr.linkit.api.internal.concurrency.{WorkerPool, InternalWorkerThread}
import fr.linkit.engine.internal.concurrency.SimpleAsyncTask

import scala.util.Failure

class HiredWorker(override val thread: Thread, override val pool: WorkerPool)
        extends AbstractWorker with InternalWorkerThread { that =>

    private final val rootTask = new SimpleAsyncTask[Nothing](-1, null, () => Failure[Nothing](null)) {
        setWorker(that)
    }

    override def getCurrentTask: Some[ThreadTask] = super.getCurrentTask match {
        case Some(value) => Some(value)
        case None        => Some(rootTask)
    }

}
