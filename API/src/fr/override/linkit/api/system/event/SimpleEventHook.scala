package fr.`override`.linkit.api.system.event

import fr.`override`.linkit.api.concurrency.{ProvidedLock, relayWorkerExecution}
import fr.`override`.linkit.api.system.event.EventHook
import fr.`override`.linkit.api.utils.ConsumerContainer
import org.jetbrains.annotations.NotNull

class SimpleEventHook[E] extends EventHook[E] {
    private val providedLock = new ProvidedLock()
    private val consumers = ConsumerContainer[E]

    @relayWorkerExecution
    override def await(@NotNull lock: AnyRef = new Object): Unit = {
        addOnce {
            providedLock.cancelCurrentProviding()
        }
        providedLock.provide(lock)
    }

    override def add(action: E => Unit): Unit = consumers += action

    override def addOnce(action: E => Unit): Unit = consumers +:+= action

    override def cancel(): Unit = {
        consumers.clear()
        providedLock.cancelAllProviding()
    }

    override def add(action: => Unit): Unit = add(_ => action)

    override def addOnce(action: => Unit): Unit = addOnce(_ => action)

    override def executeEvent(t: E): Unit = consumers.applyAll(t)
}

object SimpleEventHook {
    def apply[E](methods: E => Unit*): SimpleEventHook[E] = {
        val hook = new SimpleEventHook[E]()
        methods.foreach(hook.add)
        hook
    }
}
