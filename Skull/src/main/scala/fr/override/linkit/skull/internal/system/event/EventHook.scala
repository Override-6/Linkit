package fr.`override`.linkit.skull.internal.system.event

trait EventHook[L <: EventListener, E <: Event[_, L]] {

    def await(lock: AnyRef = this): Unit //Would wait until the hooked event triggers

    def add(action: E => Unit): Unit //would add an action to execute every times the event fires

    def add(action: => Unit): Unit

    def addOnce(action: E => Unit): Unit //would add an action to execute every times the event fires

    def addOnce(action: => Unit): Unit

    def executeEvent(event: E, listeners: Seq[L]): Unit
}
