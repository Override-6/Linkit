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

package fr.`override`.linkit.core.local

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
    def timedWait(lock: AnyRef, timeout: Long): Long = lock.synchronized {
        val t0 = now()
        lock.wait(timeout)
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
