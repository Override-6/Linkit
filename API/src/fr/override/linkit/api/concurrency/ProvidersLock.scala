package fr.`override`.linkit.api.concurrency

import java.util
import scala.collection.mutable

/**
 * This class handles a list of lock objects per threads. <br>
 * Each [[RelayWorkerThreadPool.RelayThread]] is linked too a [[ThreadLocks]] object, that contains a list of
 * objects ordered by their providing order.
 * <p>
 *     The locks are removed once the current relay thread stopped execute all the tasks it has do do during providing.
 *     as a providing could occur during a providing, the locks had to be handled in a specialized class.
 *     This class only notifies locks once a task is submitted to the thread pool.
 *     The notified thread will then execute the submitted task, or stop providing if it has to.
 * */
class ProvidersLock {

    private val providers = mutable.Map.empty[Thread, ThreadLocks]

    def addProvidingLock(lock: AnyRef): Unit = {
        val locks = providers.getOrElseUpdate(currentThread, new ThreadLocks(currentThread))
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

    private class ThreadLocks(val owner: Thread) {
        private val queue = new util.ArrayDeque[AnyRef]()

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
                //println(s"In Synchronized (current: $currentThread, lock: $lock)")
                lock.notify()
                //println(s"Done exec sync block ! (current: $currentThread, lock: $lock)")
            }
        }

        def isEmpty: Boolean = queue.isEmpty

        override def toString: String = queue.toString

    }

}
