/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.spi.gnom.packet

import fr.linkit.api.gnom.packet.{PacketAttributes, PacketAttributesPresence}
import fr.linkit.engine.gnom.packet.SimplePacketAttributes

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

