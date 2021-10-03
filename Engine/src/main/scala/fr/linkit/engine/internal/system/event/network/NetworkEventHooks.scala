/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.system.event.network

import fr.linkit.api.internal.system.event.EventHookCategory
import fr.linkit.engine.internal.system.event.SimpleEventHook
import fr.linkit.engine.internal.system.event.network.NetworkEvents._

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
