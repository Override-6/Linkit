package fr.`override`.linkit.api.concurrency

import org.jetbrains.annotations.NotNull

import scala.collection.mutable


class BusyLock(condition: => Boolean = true) {

    private val locks = mutable.HashMap[Thread, WorkTicket]()

    @relayWorkerExecution
    def keepBusy(@NotNull lock: AnyRef = new Object): Unit = {
        RelayWorkerThreadPool.checkCurrentIsWorker("Could not perform an occuped lock in a non relay thread.")
        val pool = RelayWorkerThreadPool.currentPool().get

        val ticket = locks.getOrElseUpdate(currentThread, WorkTicket())

        locks.put(currentThread, ticket)
        ticket.markBusy(lock)

        pool.keepBusyWhileOrWait(lock, ticket.mustContinueWork && condition)
        locks.remove(currentThread)
    }

    def isCurrentThreadBusy: Boolean = {
        locks.get(currentThread).exists(_.mustContinueWork)
    }

    def release(): Unit = {
        locks.get(currentThread).tapEach(_.stopWorking())
    }

    def releaseAll(): Unit = {
        locks.foreachEntry((_, t) => t.stopAllWork())
        locks.clear()
    }


    @relayWorkerExecution
    def currentPool: RelayWorkerThreadPool = {
        RelayWorkerThreadPool.checkCurrentIsWorker("Could not perform this action in a non relay thread.")
        RelayWorkerThreadPool.currentPool().get
    }

    case class WorkTicket() {
        private val depths = mutable.HashMap.empty[Int, AnyRef]

        def stopWorking(): Unit = {
            val depth = currentPool.currentProvidingDepth
            val lock = depths(depth)
            lock.synchronized {
                lock.notifyAll()
            }
            depths -= depth
        }

        def stopAllWork(): Unit = depths.clone().foreach {
            _ => stopWorking()
        }

        def markBusy(lock: AnyRef): Unit = {
            depths.put(currentPool.currentProvidingDepth, lock)
        }

        def mustContinueWork: Boolean = {
            depths.contains(currentPool.currentProvidingDepth)
        }
    }

}
