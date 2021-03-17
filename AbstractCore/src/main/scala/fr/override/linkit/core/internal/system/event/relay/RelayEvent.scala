package fr.`override`.linkit.core.internal.system.event.relay

import fr.`override`.linkit.skull.internal.system.event.{Event, EventHook}

trait RelayEvent extends Event[RelayEventHooks, RelayEventListener] {
    protected type RelayEventHook = EventHook[RelayEventListener, this.type]

    override def getHooks(category: RelayEventHooks): Array[RelayEventHook]
}
