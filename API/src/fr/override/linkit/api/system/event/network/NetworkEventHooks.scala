package fr.`override`.linkit.api.system.event.network

import fr.`override`.linkit.api.system.event.SimpleEventHook
import fr.`override`.linkit.api.system.event.network.NetworkEvents._

//noinspection TypeAnnotation
object NetworkEventHooks {

    type L <: NetworkEventListener

    val EntityAdded = SimpleEventHook[L, EntityAddedEvent](_.onEntityAdded(_))

    val EntityRemoved = SimpleEventHook[L, EntityRemovedEvent](_.onEntityRemoved(_))

    val EntityStateChange = SimpleEventHook[L, EntityStateChangeEvent](_.onEntityStateChange(_))

    val EntityEditCurrentProperties = SimpleEventHook[L, RemotePropertyChangeEvent](_.onEntityEditCurrentProperties(_))

    val EditEntityProperties = SimpleEventHook[L, RemotePropertyChangeEvent](_.onEditEntityProperties(_))

    val RemotePrintReceived = SimpleEventHook[L, RemotePrintEvent](_.onRemotePrintReceived(_))

    val RemotePrintSent = SimpleEventHook[L, RemotePrintEvent](_.onRemotePrintSent(_))

}
