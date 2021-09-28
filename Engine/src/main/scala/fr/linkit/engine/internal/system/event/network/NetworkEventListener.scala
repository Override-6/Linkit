/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.internal.system.event.network

import fr.linkit.api.internal.system.event.EventListener
import fr.linkit.engine.internal.system.event.network.NetworkEvents._

abstract class NetworkEventListener extends EventListener {

    def onEntityAdded(event: EntityAddedEvent): Unit = ()

    def onEntityRemoved(event: EntityRemovedEvent): Unit = ()

    def onEntityStateChange(event: EntityStateChangeEvent): Unit = ()

    def onEntityEditCurrentProperties(event: RemotePropertyChangeEvent): Unit = ()

    def onEditEntityProperties(event: RemotePropertyChangeEvent): Unit = ()

    def onRemotePrintReceived(event: RemotePrintEvent): Unit = ()

    def onRemotePrintSent(event: RemotePrintEvent): Unit = ()

}
