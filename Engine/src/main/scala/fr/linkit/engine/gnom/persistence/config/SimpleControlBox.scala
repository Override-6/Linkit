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

package fr.linkit.engine.gnom.persistence.config

import fr.linkit.api.gnom.persistence.context.ControlBox
import fr.linkit.api.internal.concurrency.Procrastinator

class SimpleControlBox extends ControlBox {

    private final var asyncTasks = 0

    /**
     * informs the control box that an async task will be performed.
     * */
    private def beginTask(): Unit = this.synchronized {
        asyncTasks += 1
    }

    /**
     * Informs the control box that an async tas has ended.
     * */
    private def releaseTask(): Unit = this.synchronized {
        asyncTasks -= 1
        if (asyncTasks == 0) {
            this.notifyAll()
        }
    }


    override def warpTask(procrastinator: Procrastinator)(task: => Unit): Unit = {
        beginTask()
        procrastinator.runLater {
            task
            releaseTask()
        }
    }

    override def join(): Unit = this.synchronized {
        if (asyncTasks != 0)
            wait()
    }
}
