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

package fr.linkit.engine.internal

import java.util.concurrent.locks.LockSupport

/**
 * This package is a simple utility set for aliases and concurrency operations.
 * */
package object concurrency {

    /**
     * Handles a monitor lock on the provided reference, excepted that
     * the time the thread had wait the monitor of the object is returned.
     *
     * @param lock the object to wait.
     * @return the time the thread waited on the object.
     * */
    def timedWait(lock: AnyRef): Long = lock.synchronized {
        val t0 = now()
        lock.wait()
        val t1 = now()
        t1 - t0
    }

    /**
     * Handles a monitor lock on the provided reference, excepted that
     * the time the thread had wait the monitor of the object is returned.
     *
     * @param lock    the object to wait.
     * @param timeout the maximum amount of time to wait
     * @return the time the thread waited on the object.
     * */
    def timedPark(ref: AnyRef = null, timeout: Long): Long = {
        val t0 = now()
        LockSupport.parkNanos(ref, timeout * 1000000)
        val t1 = now()
        t1 - t0
    }

    /**
     * Alias for [[System.currentTimeMillis]]
     * @return the current time in milliseconds since midnight, January 1, 1970 UTC
     * */
    def now(): Long = System.currentTimeMillis()

    /**
     * Alias for [[Thread.currentThread]]
     * @return the current Java Thread reference
     * */
    def currentThread: Thread = Thread.currentThread()

}
