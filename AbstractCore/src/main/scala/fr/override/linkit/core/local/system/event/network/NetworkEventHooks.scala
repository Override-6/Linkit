package fr.`override`.linkit.core.local.system.event.network

import fr.`override`.linkit.api.local.system.event.network.NetworkEvents._
import fr.`override`.linkit.api.local.system.event.{EventHookCategory, SimpleEventHook}

//noinspection TypeAnnotation
class NetworkEventHooks extends EventHookCategory {

    type L = NetworkEventListener

    val entityAdded = SimpleEventHook[L, EntityAddedEvent](_.onEntityAdded(_))

    val entityRemoved = SimpleEventHook[L, EntityRemovedEvent](_.onEntityRemoved(_))

    val entityStateChange = SimpleEventHook[L, EntityStateChangeEvent](_.onEntityStateChange(_))

    val entityEditCurrentProperties = SimpleEventHook[L, RemotePropertyChangeEvent](_.onEntityEditCurrentProperties(_))

    val editEntityProperties = SimpleEventHook[L, RemotePropertyChangeEvent](_.onEditEntityProperties(_))

    val remotePrintReceived = SimpleEventHook[L, RemotePrintEvent](_.onRemotePrintReceived(_))

    val remotePrintSent = SimpleEventHook[L, RemotePrintEvent](_.onRemotePrintSent(_))

}
