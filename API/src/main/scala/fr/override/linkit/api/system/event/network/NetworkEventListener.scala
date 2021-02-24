package fr.`override`.linkit.api.system.event.network

import fr.`override`.linkit.api.system.event.EventListener
import fr.`override`.linkit.api.system.event.network.NetworkEvents._

abstract class NetworkEventListener extends EventListener {

    def onEntityAdded(event: EntityAddedEvent): Unit = ()

    def onEntityRemoved(event: EntityRemovedEvent): Unit = ()

    def onEntityStateChange(event: EntityStateChangeEvent): Unit = ()

    def onEntityEditCurrentProperties(event: RemotePropertyChangeEvent): Unit = ()

    def onEditEntityProperties(event: RemotePropertyChangeEvent): Unit = ()

    def onRemotePrintReceived(event: RemotePrintEvent): Unit = ()

    def onRemotePrintSent(event: RemotePrintEvent): Unit = ()

}
