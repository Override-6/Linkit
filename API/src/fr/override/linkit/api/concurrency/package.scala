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

package fr.`override`.linkit.api

package object concurrency {

    def timedWait(lock: AnyRef): Long = lock.synchronized {
        val t0 = now()
        lock.wait()
        val t1 = now()
        t1 - t0
    }

    def timedWait(lock: AnyRef, timeout: Long): Long = lock.synchronized {
        val t0 = now()
        lock.wait(timeout)
        val t1 = now()
        t1 - t0
    }

    def now(): Long = System.currentTimeMillis()

    def currentThread: Thread = Thread.currentThread()

}
