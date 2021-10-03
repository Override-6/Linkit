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

package fr.linkit.api.internal.system.event

import fr.linkit.api.internal.concurrency.workerExecution

trait EventHook[L <: EventListener, E <: Event[_, L]] {

    @workerExecution
    def await(): Unit //Would wait until the hooked event triggers

    def add(action: E => Unit): Unit //would add an action to execute every times the event fires

    def add(action: => Unit): Unit

    def addOnce(action: E => Unit): Unit //would add an action to execute every times the event fires

    def addOnce(action: => Unit): Unit

    def executeEvent(event: E, listeners: Seq[L]): Unit
}
