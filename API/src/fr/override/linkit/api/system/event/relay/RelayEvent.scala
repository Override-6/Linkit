package fr.`override`.linkit.api.system.event.relay

import fr.`override`.linkit.api.system.event.{Event, EventHook}

trait RelayEvent extends Event[RelayEventListener] {
    protected type RelayEventHook = EventHook[this.type, RelayEventListener]

    override def getHooks: Array[RelayEventHook]
}
