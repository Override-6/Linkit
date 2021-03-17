package fr.`override`.linkit.core.internal.system.event.relay

import fr.`override`.linkit.skull.internal.system.event.relay.RelayEvents.{ConnectionStateEvent, OrderReceivedEvent, RelayStateEvent}
import fr.`override`.linkit.skull.internal.system.event.{EventHookCategory, SimpleEventHook}

//noinspection TypeAnnotation
class RelayEventHooks extends EventHookCategory {

    type L = RelayEventListener

    val disconnected = SimpleEventHook[L, ConnectionStateEvent]((l, _) => l.onDisconnected(), _.onConnectionStateChange(_))

    val connected = SimpleEventHook[L, ConnectionStateEvent]((l, _) => l.onConnected(), _.onConnectionStateChange(_))

    val connecting = SimpleEventHook[L, ConnectionStateEvent]((l, _) => l.onConnecting(), _.onConnectionStateChange(_))

    val crashed = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onCrashed(), _.onStateChange(_))

    val ready = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onReady(), _.onStateChange(_))

    val closed = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onClosed(), _.onStateChange(_))

    val connectionClosed = SimpleEventHook[L, ConnectionStateEvent]((l, _) => l.onConnectionClosed(), _.onConnectionStateChange(_))

    val start = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onStart(), _.onStateChange(_))

    val stateChange = SimpleEventHook[L, RelayStateEvent](_.onStateChange(_))

    val connectionStateChange = SimpleEventHook[L, ConnectionStateEvent](_.onConnectionStateChange(_))

    val orderReceived = SimpleEventHook[L, OrderReceivedEvent](_.onOrderReceived(_))

}
