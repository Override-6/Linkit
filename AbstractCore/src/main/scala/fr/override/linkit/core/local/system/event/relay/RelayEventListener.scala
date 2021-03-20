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

import fr.`override`.linkit.api.local.system.event.EventListener
import fr.`override`.linkit.core.local.system.event.relay.RelayEvents._

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
