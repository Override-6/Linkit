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

package fr.linkit.core.connection.packet.serialization.strategies

import fr.linkit.api.connection.packet.serialization.strategy.{SerialStrategy, StrategicSerializer}
import fr.linkit.core.connection.packet.SimplePacketAttributes

object PacketAttributesStrategy extends SerialStrategy[SimplePacketAttributes] {

    override def getTypeHandling: Class[SimplePacketAttributes] = classOf[SimplePacketAttributes]

    override def serial(instance: SimplePacketAttributes, serializer: StrategicSerializer): Array[Byte] = {
        val tuples = instance.attributes.toArray
        serializer.serialize(tuples, false)
    }

    override def deserial(bytes: Array[Byte], serializer: StrategicSerializer): Any = {
        val tuples = serializer.deserializeObject(bytes, classOf[Array[(String, Serializable)]])
        SimplePacketAttributes.from(tuples: _*)
    }
}
