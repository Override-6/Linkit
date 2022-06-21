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

package fr.linkit.engine.internal.concurrency.pool

class EquilibratedWorkerController extends SimpleWorkerController {
    
    private var pendingWakeups = 0
    
    override protected def createControlTicket(pauseCondition: => Boolean): Unit = {
        this.synchronized {
            if (!pauseCondition && pendingWakeups > 0) {
                pendingWakeups -= 1
                return
            }
        }
        super.createControlTicket(pauseCondition)
    }
    
    override def wakeupAnyTask(): Unit = {
        if (tickets.nonEmpty) {
            super.wakeupAnyTask()
            return
        }
        this.synchronized {
            pendingWakeups += 1
        }
    }
    
    def clearPendingWakeups: Unit = pendingWakeups = 0
}
