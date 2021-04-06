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

package fr.linkit.core.connection.packet

import fr.linkit.api.connection.packet.Bundle
import fr.linkit.api.connection.packet.traffic.PacketChannel

abstract class AbstractBundle(channel: PacketChannel) extends Bundle {

    override def getChannel: PacketChannel = channel

    override def store(): Unit = channel.storeBundle(this)

    override def storeInParent(): Unit = {
        val parent = channel.getParent
        if (parent.isDefined)
            parent.get.storeBundle(this)
    }

    override def storeInAllParents(): Unit = {
        var lastParent = channel.getParent
        while (lastParent.isDefined) {
            val parent = lastParent.get
            parent.storeBundle(this)
            lastParent = parent.getParent
        }
    }
}
