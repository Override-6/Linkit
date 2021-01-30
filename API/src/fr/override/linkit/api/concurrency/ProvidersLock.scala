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
