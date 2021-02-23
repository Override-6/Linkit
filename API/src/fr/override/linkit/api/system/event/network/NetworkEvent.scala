package fr.`override`.linkit.api.system.event.network

import fr.`override`.linkit.api.network.NetworkEntity
import fr.`override`.linkit.api.system.event.{Event, EventHook}

trait NetworkEvent extends Event[NetworkEventListener] {
    protected type NetworkEventHook = EventHook[NetworkEvent.this.type, NetworkEventListener]
    val entity: NetworkEntity

    override def getHooks: Array[NetworkEventHook]
}
