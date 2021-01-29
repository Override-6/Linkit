package fr.`override`.linkit.api.concurrency

import java.util
import java.util.concurrent.ThreadLocalRandom

import scala.collection.mutable

class ProvidersLock {

    private val providers = mutable.Map.empty[Thread, ThreadLocks]
    private val random = ThreadLocalRandom.current()

    def addProvidingLock(lock: AnyRef): Unit = {
        providers.getOrElseUpdate(currentThread, new ThreadLocks)
                .addLock(lock)
    }

    def removeProvidingLock(): AnyRef = {
        val locks = providers.getOrElseUpdate(currentThread, new ThreadLocks)
        val lock = locks.removeLock()
        if (locks.isEmpty) {
            providers.remove(currentThread)
        }

        lock
    }

    def notifyOneProvider(): Unit = {
        val randIndex = random.nextInt(0, providers.size)
        providers.values.toList(randIndex).notifyLock()
    }

    private class ThreadLocks {
        private val locks = new util.ArrayDeque[AnyRef]()
        @volatile private var notified = false

        def addLock(lock: AnyRef): Unit = {
            notified = false
            locks.addLast(lock)
        }

        def removeLock(): AnyRef = {
            notified = false
            locks.removeLast()
        }

        def isEmpty: Boolean = locks.isEmpty

        def notifyLock(): Unit = {
            val lock = locks.peekLast()
            lock.synchronized {
                notified = true
                lock.notify()
            }
        }

    }

}
