package fr.`override`.linkit.api.concurrency

import org.jetbrains.annotations.NotNull

import scala.collection.mutable


class ProvidedLock(condition: => Boolean = true) {

    private val locks = mutable.HashMap[Thread, ProvideTicket]()

    @relayWorkerExecution
    def provide(@NotNull lock: AnyRef = new Object): Unit = {
        RelayWorkerThreadPool.checkCurrentIsWorker("Could not perform a provided lock in a non relay thread.")
        val pool = RelayWorkerThreadPool.currentPool().get

        val ticket = locks.getOrElseUpdate(currentThread, ProvideTicket())

        locks.put(currentThread, ticket)
        ticket.markProviding(lock)

        pool.provideWhileOrWait(lock, ticket.mustContinue && condition)
        locks.remove(currentThread)
    }

    def isCurrentThreadProviding: Boolean = {
        locks.get(currentThread).exists(_.mustContinue)
    }

    def cancelCurrentProviding(): Unit = {
        locks.get(currentThread).tapEach(_.stopProviding())
    }

    def cancelAllProviding(): Unit = {
        locks.foreachEntry((_, t) => t.stopAllProviding())
        locks.clear()
    }


    @relayWorkerExecution
    def currentPool: RelayWorkerThreadPool = {
        RelayWorkerThreadPool.checkCurrentIsWorker("Could not perform this action in a non relay thread.")
        RelayWorkerThreadPool.currentPool().get
    }

    case class ProvideTicket() {
        private val depths = mutable.HashMap.empty[Int, AnyRef]

        def stopProviding(): Unit = {
            val depth = currentPool.currentProvidingDepth
            val lock = depths(depth)
            lock.synchronized {
                lock.notifyAll()
            }
            depths -= depth
        }

        def stopAllProviding(): Unit = depths.clone().foreach {
            _ => stopProviding()
        }

        def markProviding(lock: AnyRef): Unit = {
            depths.put(currentPool.currentProvidingDepth, lock)
        }

        def mustContinue: Boolean = {
            depths.contains(currentPool.currentProvidingDepth)
        }
    }

}
