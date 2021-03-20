/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.core.local.system.event.relay

import fr.`override`.linkit.api.local.system.event.EventHookCategory
import fr.`override`.linkit.core.local.system.event.SimpleEventHook
import fr.`override`.linkit.core.local.system.event.relay.RelayEvents._

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
