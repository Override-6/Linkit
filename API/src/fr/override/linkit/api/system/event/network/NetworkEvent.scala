package fr.`override`.linkit.api.system.event.network

import fr.`override`.linkit.api.network.NetworkEntity
import fr.`override`.linkit.api.system.event.Event

trait NetworkEvent extends Event[NetworkEventListener] {
    val entity: NetworkEntity
}
