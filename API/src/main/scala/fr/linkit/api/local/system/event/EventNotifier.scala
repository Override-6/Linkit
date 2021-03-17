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

package fr.linkit.api.local.system.event

trait EventNotifier {

    def register(listener: EventListener): Unit

    def unregister(listener: EventListener): Unit

    def notifyEvent[C <: EventHookCategory, L <: EventListener](hookCategory: C, event: Event[C, L]): Unit

    def notifyEvent[C <: EventHookCategory, L <: EventListener](event: Event[C, L])(implicit hookCategory: C): Unit

}
