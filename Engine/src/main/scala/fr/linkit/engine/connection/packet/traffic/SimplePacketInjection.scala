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

package fr.linkit.engine.connection.packet.traffic

import fr.linkit.api.connection.packet.PacketBundle
import fr.linkit.api.connection.packet.traffic.PacketInjection

class SimplePacketInjection(override val bundle: PacketBundle) extends PacketInjection {

    private val path = bundle.coords.path
    private val limit = path.length - 1
    private var index: Int = -1

    override def nextIdentifier: Int = {
        index += 1
        path(index)
    }

    override def haveMoreIdentifier: Boolean = {
        index >= limit
    }
}
