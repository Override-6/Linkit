package fr.`override`.linkit.api.system.evente.relay

import fr.`override`.linkit.api.system.evente.{Event, EventHook}

trait RelayEvent extends Event[RelayEventHooks, RelayEventListener] {
    protected type RelayEventHook = EventHook[RelayEventListener, this.type]

    override def getHooks(category: RelayEventHooks): Array[RelayEventHook]
}
