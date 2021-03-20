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

import java.util
import scala.collection.mutable

class ProvidersLock {

    private val providers = mutable.Map.empty[Thread, ThreadLocks]

    def addProvidingLock(lock: AnyRef): Unit = {
        val locks = providers.getOrElseUpdate(currentThread, new ThreadLocks)
        locks.addLock(lock)
        //println(s"locks = ${locks} ($currentThread) - ADDED")
    }

    def removeProvidingLock(): AnyRef = {
        val locksOpt = providers.get(currentThread)
        if (locksOpt.isEmpty)
            return null

        val locks = locksOpt.get
        val lock = locks.removeLock()

        if (locks.isEmpty) {
            providers.remove(currentThread) //will unregister this lock.
        }
        lock
    }

    def notifyOneProvider(): Unit = {
        if (providers.isEmpty)
            return
        providers
                .values
                .find(_.owner.getState == Thread.State.WAITING)
                .foreach(_.notifyLock())
    }

    def isProviding: Boolean = {
        providers.contains(currentThread)
    }

    private class ThreadLocks {
        val owner: Thread = currentThread
        private val queue = new util.ArrayDeque[AnyRef]()
        @volatile private var inSync = false

        def addLock(lock: AnyRef): Unit = {
            queue.addLast(lock)
        }

        def removeLock(): AnyRef = {
            queue.removeLast()
        }

        def notifyLock(): Unit = {
            if (isEmpty)
                return

            val lock = queue.getLast
            //println(s"NOTIFYING... (current: $currentThread, lock: $lock, owner: $owner, inSync: $inSync)")
            lock.synchronized {
                inSync = true
                //println(s"In Synchronized (current: $currentThread, lock: $lock)")
                lock.notify()
                //println(s"Done exec sync block ! (current: $currentThread, lock: $lock)")
                inSync = false
            }
        }

        def isEmpty: Boolean = queue.isEmpty

        override def toString: String = queue.toString

    }

}
