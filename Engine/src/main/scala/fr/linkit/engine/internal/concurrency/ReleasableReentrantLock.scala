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

package fr.linkit.engine.internal.concurrency

import java.util.concurrent.locks.ReentrantLock

class ReleasableReentrantLock extends ReentrantLock {

    def release(): Int = {
        if (!isHeldByCurrentThread)
            throw new IllegalMonitorStateException("Current thread is not holding this lock.")
        val count = getHoldCount
        for (_ <- 0 until count)
            unlock()
        count
    }

    def depthLock(depth: Int): Unit = {
        for (_ <- 0 to depth)
            lock()
    }

}