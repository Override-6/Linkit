package fr.`override`.linkit.api.system.evente.relay

import fr.`override`.linkit.api.system.evente.EventListener
import fr.`override`.linkit.api.system.evente.relay.RelayEvents.{OrderReceivedEvent, RelayStateEvent}

abstract class RelayEventListener extends EventListener {

    def onConnected(): Unit = ()

    def onDisconnected(): Unit = ()

    def onConnecting(): Unit = ()

    def onCrashed(): Unit = ()

    def onReady(): Unit = ()

    def onClosed(): Unit = ()

    def onStart(): Unit = ()

    def onStateChange(event: RelayStateEvent): Unit = ()

    def onOrderReceived(event: OrderReceivedEvent): Unit = ()

}
