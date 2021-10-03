/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.internal.system.event.network

import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.internal.system.event.{Event, EventHook}

trait NetworkEvent extends Event[NetworkEventHooks, NetworkEventListener] {

    protected type NetworkEventHook <: EventHook[_ <: NetworkEventListener, _ <: Event[_ <: NetworkEventHooks, _ <: NetworkEventListener]]
    val entity: Engine

    override def getHooks(category: NetworkEventHooks): Array[NetworkEventHook]

}
