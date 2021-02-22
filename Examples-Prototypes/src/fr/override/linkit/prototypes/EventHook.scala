package fr.`override`.linkit.prototypes

trait EventHook {

    def await(): Unit //Would wait until the hooked event triggers

    def on(action: => Unit): Unit //would add an action to execute once the event fires

    def cancel(): Unit //Would cancel this hook (don't execute anything, stop waiting if awaited)

    def consume(): Boolean //If the event is a kind of Consumable, the execution associated with the event would not occur. Return true if the event was a consumable event

}
