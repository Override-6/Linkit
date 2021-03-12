package fr.`override`.linkit.api.system.evente.relay

import fr.`override`.linkit.api.system.evente.relay.RelayEvents.{OrderReceivedEvent, RelayStateEvent}
import fr.`override`.linkit.api.system.evente.{EventHookCategory, SimpleEventHook}

//noinspection TypeAnnotation
class RelayEventHooks extends EventHookCategory {

    type L = RelayEventListener

    val disconnected = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onDisconnected(), _.onStateChange(_))

    val connecting = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onConnecting(), _.onStateChange(_))

    val crashed = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onCrashed(), _.onStateChange(_))

    val ready = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onReady(), _.onStateChange(_))

    val closed = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onClosed(), _.onStateChange(_))

    val start = SimpleEventHook[L, RelayStateEvent]((l, _) => l.onStart(), _.onStateChange(_))

    val stateChange = SimpleEventHook[L, RelayStateEvent](_.onStateChange(_))

    val orderReceived = SimpleEventHook[L, OrderReceivedEvent](_.onOrderReceived(_))

}
