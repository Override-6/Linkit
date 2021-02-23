package fr.`override`.linkit.api.concurrency

import org.jetbrains.annotations.NotNull

import scala.collection.mutable


class ProvidedLock(condition: => Boolean = true) {

    private val locks = mutable.HashMap[Thread, ProvideTicket]()

    @relayWorkerExecution
    def provide(@NotNull lock: AnyRef = new Object): Unit = {
        RelayWorkerThreadPool.checkCurrentIsWorker("Could not perform a provided lock in a non relay thread.")
        val pool = RelayWorkerThreadPool.currentPool().get

        val ticket = locks.getOrElseUpdate(currentThread, ProvideTicket(lock))

        locks.put(currentThread, ticket)
        ticket.markProviding()

        pool.provideWhileOrWait(lock, ticket.mustContinue && condition)
        locks.remove(currentThread)
    }

    def isCurrentThreadProviding: Boolean = {
        locks.get(currentThread).exists(_.mustContinue)
    }

    def cancelCurrentProviding(): Unit = {
        locks.get(currentThread).tapEach(_.unmarkProviding())
    }

    def cancelAllProviding(): Unit = {
        locks.remove(currentThread)
    }


    @relayWorkerExecution
    def currentPool: RelayWorkerThreadPool = {
        RelayWorkerThreadPool.checkCurrentIsWorker("Could not perform this action in a non relay thread.")
        RelayWorkerThreadPool.currentPool().get
    }

    case class ProvideTicket(lock: AnyRef) {
        private val depths = mutable.HashSet.empty[Int]

        def unmarkProviding(): Unit = depths -= currentPool.currentProvidingDepth

        def markProviding(): Unit = depths += currentPool.currentProvidingDepth

        def mustContinue: Boolean = {
            depths.contains(currentPool.currentProvidingDepth)
        }
    }

}
