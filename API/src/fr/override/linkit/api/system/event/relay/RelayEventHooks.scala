package fr.`override`.linkit.api.system.event.relay

import fr.`override`.linkit.api.system.event.SimpleEventHook
import fr.`override`.linkit.api.system.event.relay.RelayEvents.{OrderReceivedEvent, RelayStateEvent}

//noinspection TypeAnnotation
object RelayEventHooks {

    type L = RelayEventListener

    val Connected = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onConnected(), _.onStateChange(_))

    val Disconnected = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onDisconnected(), _.onStateChange(_))

    val Connecting = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onConnecting(), _.onStateChange(_))

    val Crashed = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onCrashed(), _.onStateChange(_))

    val Ready = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onReady(), _.onStateChange(_))

    val Closed = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onClosed(), _.onStateChange(_))

    val Start = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onStart(), _.onStateChange(_))

    val StateChange = SimpleEventHook[L, RelayStateEvent](_.onStateChange(_))

    val OrderReceived = SimpleEventHook[L, OrderReceivedEvent](_.onOrderReceived(_))

}
