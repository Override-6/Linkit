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

package fr.linkit.engine.connection.packet.persistence

import fr.linkit.api.connection.packet.persistence.Serializer
import fr.linkit.engine.connection.packet.persistence.v3.DefaultPersistenceContext
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.DefaultDeserialisationInputStream
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.{DefaultSerialisationOutputStream, DefaultSerialisationObjectPool}

import java.nio.ByteBuffer

class DefaultSerializer() extends Serializer {

    private  val context                = new DefaultPersistenceContext
    override val signature: Array[Byte] = Array(4)

    override def serialize(serializable: Serializable, withSignature: Boolean): Array[Byte] = {
        val pool     = new DefaultSerialisationObjectPool()
        val out1     = new DefaultSerialisationOutputStream(ByteBuffer.allocate(10000), pool, context)
        val out2     = new DefaultSerialisationOutputStream(ByteBuffer.allocate(10000), pool, context)
        val rootNode = out2.progression.getSerializationNode(serializable)
        rootNode.writeBytes(out2)
        pool.writePool(out1)
        out1.put(out2.array(), 0, out2.position()).array().take(out1.position())
    }

    override def isSameSignature(bytes: Array[Byte]): Boolean = bytes.startsWith(signature)

    override def deserialize(bytes: Array[Byte]): Any = {
        val in = new DefaultDeserialisationInputStream(ByteBuffer.wrap(bytes), context)
        in.progression.getNextDeserializationNode
                .deserialize(in)
    }

    override def deserializeAll(bytes: Array[Byte]): Array[Any] = {
        deserialize(bytes).asInstanceOf[Array[Any]]
    }

}
