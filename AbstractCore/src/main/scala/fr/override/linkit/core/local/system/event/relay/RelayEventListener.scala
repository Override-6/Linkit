package fr.`override`.linkit.core.local.system.event.relay

import fr.`override`.linkit.api.local.system.event.relay.RelayEvents.{ConnectionStateEvent, OrderReceivedEvent, RelayStateEvent}

abstract class RelayEventListener extends EventListener {

    def onConnected(): Unit = ()

    def onDisconnected(): Unit = ()

    def onConnecting(): Unit = ()

    def onCrashed(): Unit = ()

    def onReady(): Unit = ()

    def onClosed(): Unit = ()

    def onConnectionClosed(): Unit = ()

    def onStart(): Unit = ()

    def onStateChange(event: RelayStateEvent): Unit = ()

    def onConnectionStateChange(event: ConnectionStateEvent): Unit = ()

    def onOrderReceived(event: OrderReceivedEvent): Unit = ()

}
