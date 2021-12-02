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

package fr.linkit.engine.gnom.persistence.context

import fr.linkit.api.gnom.persistence.context.ControlBox
import fr.linkit.api.internal.concurrency.WorkerPools
import fr.linkit.engine.internal.concurrency.pool.SimpleWorkerController

class SimpleControlBox extends ControlBox {

    private final var asyncTasks = 0
    private final val locker     = new SimpleWorkerController()

    /**
     * informs the control box that an async task will be performed.
     * */
    override def beginTask(): Unit = this.synchronized {
        asyncTasks += 1
    }

    /**
     * Informs the control box that an async tas has ended.
     * */
    override def releaseTask(): Unit = this.synchronized {
        asyncTasks -= 1
        if (asyncTasks == 0) {
            locker.wakeupAllTasks()
            this.notifyAll()
        }
    }

    override def join(): Unit = {
        val currentTask = this.synchronized {
            if (asyncTasks == 0)
                return
            WorkerPools.currentTask.orNull
        }
        if (currentTask == null) this.wait()
        else locker.pauseCurrentTask()
    }
}
