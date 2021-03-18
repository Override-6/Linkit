package fr.`override`.linkit.core.local.system.event.network

import fr.`override`.linkit.api.connection.network.NetworkEntity
import fr.`override`.linkit.api.local.system.event.{Event, EventHook}

trait NetworkEvent extends Event[NetworkEventHooks, NetworkEventListener] {
    protected type NetworkEventHook <: EventHook[_ <: NetworkEventListener, _ <: Event[_ <: NetworkEventHooks, _ <: NetworkEventListener]]
    val entity: NetworkEntity

    override def getHooks(category: NetworkEventHooks): Array[NetworkEventHook]

}
