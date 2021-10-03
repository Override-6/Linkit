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

package fr.linkit.engine.internal.system.event

import fr.linkit.api.internal.system.event.{Event, EventHookCategory, EventListener, EventNotifier}

import scala.collection.mutable.ListBuffer

class DefaultEventNotifier extends EventNotifier {

    private val listeners = ListBuffer[EventListener]()

    override def register(listener: EventListener): Unit = listeners += listener

    override def unregister(listener: EventListener): Unit = listeners -= listener

    override def notifyEvent[C <: EventHookCategory, L <: EventListener](hookCategory: C, event: Event[C, L]): Unit = {
        val eventListeners = listeners
                .filter(_.isInstanceOf[L])
                .map(_.asInstanceOf[L])
        event.getHooks(hookCategory).foreach(_.executeEvent(cast(event), cast(eventListeners.toSeq)))
    }

    override def notifyEvent[C <: EventHookCategory, L <: EventListener](event: Event[C, L])(implicit hookCategory: C): Unit = {
        val eventListeners = listeners
                .filter(_.isInstanceOf[L])
                .map(_.asInstanceOf[L])
        event.getHooks(hookCategory).foreach(_.executeEvent(cast(event), cast(eventListeners.toSeq)))
    }

    private def cast[T](any: Any): T = any.asInstanceOf[T] //FIXME remove

}
