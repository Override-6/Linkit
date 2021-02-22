package fr.`override`.linkit.api.system.event

trait EventHook[E <: Event] {

    def await(lock: AnyRef = this): Unit //Would wait until the hooked event triggers

    def add(action: E => Unit): Unit //would add an action to execute every times the event fires
    def add(action: => Unit): Unit

    def addOnce(action: E => Unit): Unit //would add an action to execute every times the event fires
    def addOnce(action: => Unit): Unit

    def cancel(): Unit //Would cancel this hook (don't execute anything, stop waiting if awaited)

    def executeEvent(t: E): Unit

}
