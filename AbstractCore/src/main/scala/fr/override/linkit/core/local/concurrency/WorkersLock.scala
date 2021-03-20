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

package fr.`override`.linkit.core.local.concurrency

import java.util
import scala.collection.mutable

/**
 * This class handles a list of lock objects per threads. <br>
 * Each [[BusyWorkerPool.RelayThread]] is linked to a [[ThreadLocks]] object, that contains a list of
 * objects ordered by their providing order.
 * <p>
 *     The locks are removed once the current relay thread stopped execute all the tasks it has do do during providing.
 *     as a providing could occur during a providing, the locks had to be handled in a specialized class.
 *     This class only notifies locks once a task is submitted to the thread pool.
 *     The notified thread will then execute the submitted task, or stop providing if it has to.
 * </p>
 * */
class WorkersLock {

    private val busyLockRefs = mutable.Map.empty[Thread, ThreadLocks]

    /**
     * Adds an object reference to the lock list
     * @param lock the reference to add
     * */
    def addBusyLock(lock: AnyRef): Unit = {
        val locks = busyLockRefs.getOrElseUpdate(currentThread, new ThreadLocks(currentThread))
        locks.addLock(lock)
    }

    /**
     * removes the last Busy Lock of the current thread
     * */
    def removeLastBusyLock(): AnyRef = {
        val locksOpt = busyLockRefs.get(currentThread)
        if (locksOpt.isEmpty)
            return null

        val locks = locksOpt.get
        val lock = locks.removeLock()

        if (locks.isEmpty) {
            busyLockRefs.remove(currentThread) //will unregister this lock.
        }
        lock
    }

    /**
     * Notifies one thread that is waiting on a registered object reference.
     * */
    def notifyOneBusyThread(): Unit = {
        if (busyLockRefs.isEmpty)
            return
        busyLockRefs
                .values
                .find(_.owner.getState == Thread.State.WAITING)
                .foreach(_.notifyLock())
    }

    /**
     * @return true if the current thread handles one or multiple busy locks
     * */
    def isCurrentThreadBusy: Boolean = {
        busyLockRefs.contains(currentThread)
    }

    /**
     * This class represents all the current BusyLocks associated with a thread
     * This class works as a LIFO Queue
     * */
    private class ThreadLocks(val owner: Thread) {
        private val queue = new util.ArrayDeque[AnyRef]()

        /**
         * Adds a lock to this queue
         * @param lock the lock reference to add
         * */
        def addLock(lock: AnyRef): Unit = {
            queue.addLast(lock)
        }

        /**
         * Removes the last lock reference of the queue
         * */
        def removeLock(): AnyRef = {
            queue.removeLast()
        }

        /**
         * Notifies the current lock monitor of this thread.
         * */
        def notifyLock(): Unit = {
            if (isEmpty)
                return

            val lock = queue.getLast
            //println(s"NOTIFYING... (current: $currentThread, lock: $lock, owner: $owner, inSync: $inSync)")
            lock.synchronized {
                //println(s"In Synchronized (current: $currentThread, lock: $lock)")
                lock.notify()
                //println(s"Done exec sync block ! (current: $currentThread, lock: $lock)")
            }
        }

        /**
         * @return true if the handled queue does not contains any busy lock
         */
        def isEmpty: Boolean = queue.isEmpty

        override def toString: String = queue.toString

    }

}
