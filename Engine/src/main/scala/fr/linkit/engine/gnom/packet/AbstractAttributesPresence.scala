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

package fr.linkit.engine.gnom.packet

import fr.linkit.api.gnom.packet.{PacketAttributes, PacketAttributesPresence}

abstract class AbstractAttributesPresence extends PacketAttributesPresence {

    protected var defaultAttributes: PacketAttributes = SimplePacketAttributes.empty

    override def addDefaultAttribute(key: Serializable, value: Serializable): this.type = {
        defaultAttributes.putAttribute(key, value)
        this
    }

    override def getDefaultAttribute[S](key: Serializable): Option[S] = {
        defaultAttributes.getAttribute(key)
    }

    override def drainAllAttributes(attributes: PacketAttributes): this.type = {
        defaultAttributes.drainAttributes(attributes)
        this
    }

    override def drainAllDefaultAttributes(attributes: PacketAttributesPresence): this.type = {
        defaultAttributes.foreachAttributes((k, v) => attributes.addDefaultAttribute(k, v))
        this
    }

}

