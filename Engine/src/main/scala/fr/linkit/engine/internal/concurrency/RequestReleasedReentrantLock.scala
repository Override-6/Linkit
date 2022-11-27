package fr.linkit.engine.internal.concurrency

import fr.linkit.engine.internal.concurrency.RequestReleasedReentrantLock.threadLocks

import java.util.concurrent.TimeUnit
import scala.collection.mutable


/**
 * A special kind of reentrant lock that gets automatically released
 * when a thread is waiting for any response comming from a channel.
 * */
class RequestReleasedReentrantLock extends ReleasableReentrantLock {

    private var threadReserved       : Thread = _
    private var threadReservationLock: Object = _

    private def lockAction[B](f: => B): B = {
        val v = f
        if (threadReserved != null) {
            // wait reservationLock to release
            threadReservationLock.synchronized {
                return f
            }
        }
        threadLocks.get() += this
        v
    }

    override def release(): Int = {
        val v = super.release()
        if (!isHeldByCurrentThread)
            threadLocks.get() -= this
        v
    }

    private def releaseReserved(reservationLock: Object): Int = {
        threadReservationLock = reservationLock
        threadReserved = Thread.currentThread() //reserve current thread in order not to let other waiting threads to acquire this thread
        release()
    }

    override def depthLock(depth: Int): Unit = lockAction {
        super.depthLock(depth)
    }

    override def lock(): Unit = lockAction {
        super.lock()
    }

    override def lockInterruptibly(): Unit = lockAction {
        super.lockInterruptibly()
    }

    override def tryLock(): Boolean = lockAction {
        super.tryLock()
    }

    override def tryLock(timeout: Long, unit: TimeUnit): Boolean = lockAction {
        super.tryLock(timeout, unit)
    }

    override def unlock(): Unit = {
        super.unlock()
        if (!isHeldByCurrentThread)
            threadLocks.get() -= this
    }

}

object RequestReleasedReentrantLock {
    private val threadLocks = ThreadLocal.withInitial(() => mutable.HashSet.empty[RequestReleasedReentrantLock])

    /**
     * will releases all RequestReleasedReentrantLocks of current thread to execute f
     * then the locks are reestablished once f completes.
     * */
    def runReleased[A](f: => A): A = {
        val locks           = threadLocks.get().clone()
        val reservationLock = new Object
        reservationLock.synchronized {
            val locksDepth = locks.map(_.releaseReserved(reservationLock))
            val v          = f
            for ((lock, depth) <- locks zip locksDepth)
                lock.depthLock(depth)
            v
        }
    }

}
