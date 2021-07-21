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

import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.persistence.Serializer
import fr.linkit.engine.connection.packet.persistence.tree.DefaultSerialContext
import fr.linkit.engine.connection.packet.persistence.v3.DefaultPersistenceContext
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.{DefaultSerialOutputStream, DefaultSerialisationProgression}

import java.nio.ByteBuffer

class DefaultSerializer() extends Serializer {

    private val context = new DefaultPersistenceContext
    override val signature: Array[Byte] = Array(4)

    override def serialize(serializable: Serializable, withSignature: Boolean): Array[Byte] = {
        val progress = new DefaultSerialisationProgression
        val out = new DefaultSerialOutputStream(ByteBuffer.allocate(50000), context)
        context.getNode(serializable, out, )
                .writeBytes()
    }

    override def isSameSignature(bytes: Array[Byte]): Boolean = bytes.startsWith(signature)

    override def deserialize(bytes: Array[Byte]): Any = {
        val node = finder.getDeserialNodeFor(bytes.drop(1))
        node.deserialize()
    }

    override def deserializeAll(bytes: Array[Byte]): Array[Any] = {
        val node = finder.getDeserialNodeFor[Array[Any]](bytes.drop(1))
        node.deserialize()
    }

    def getContext: DefaultSerialContext = context

    def initNetwork(network: Network): Unit = {
        context.updateNetwork(network)
    }
}
