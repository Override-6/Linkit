package fr.`override`.linkit.api.system.evente.network

import fr.`override`.linkit.api.network.NetworkEntity
import fr.`override`.linkit.api.system.evente.{Event, EventHook}

trait NetworkEvent extends Event[NetworkEventHooks, NetworkEventListener] {
    protected type NetworkEventHook <: EventHook[_ <: NetworkEventListener, _ <: Event[_ <: NetworkEventHooks, _ <: NetworkEventListener]]
    val entity: NetworkEntity

    override def getHooks(category: NetworkEventHooks): Array[NetworkEventHook]

}
