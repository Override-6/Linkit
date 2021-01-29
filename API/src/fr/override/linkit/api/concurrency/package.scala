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
