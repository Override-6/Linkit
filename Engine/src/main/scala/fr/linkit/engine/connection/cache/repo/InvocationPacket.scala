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

package fr.linkit.engine.connection.cache.repo

import fr.linkit.api.connection.packet.Packet

case class InvocationPacket(private val path: Array[Int], methodID: Int, params: Array[Any]) extends Packet {

    @transient private var currentStepIndex = -1

    def nextStep: Int = {
        currentStepIndex += 1
        if (!haveMoreStep)
            throw new NoSuchElementException(s"Exceeded path length ($currentStepIndex)")
        path(currentStepIndex)
    }

    def haveMoreStep: Boolean = currentStepIndex < path.length - 1

}

object InvocationPacket {

    def apply(path: Array[Int], memberID: Int, args: Array[Any]): InvocationPacket = {
        new InvocationPacket(path, memberID, args)
    }

    def unapply(arg: InvocationPacket): Option[(Int, Array[Any])] = {
        if (arg == null)
            return None
        Some((arg.methodID, arg.params))
    }
}
