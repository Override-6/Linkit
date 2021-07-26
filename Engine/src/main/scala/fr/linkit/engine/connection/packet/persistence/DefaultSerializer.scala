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
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.DefaultDeserializationInputStream
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.{DefaultSerialisationObjectPool, DefaultSerialisationOutputStream}
import java.nio.ByteBuffer

import fr.linkit.api.connection.network.Network
import fr.linkit.engine.connection.packet.persistence.v3.persistor.PuppetWrapperPersistor

import scala.collection.mutable.ListBuffer

class DefaultSerializer() extends Serializer {

    val context                = new DefaultPersistenceContext
    override val signature: Array[Byte] = Array(4)

    private var network: Network = _

    override def serialize(serializable: Serializable, withSignature: Boolean): Array[Byte] = {
        val pool     = new DefaultSerialisationObjectPool()
        val poolStream     = new DefaultSerialisationOutputStream(ByteBuffer.allocate(10000), pool, context)
        val bodyStream     = new DefaultSerialisationOutputStream(ByteBuffer.allocate(10000), pool, context)
        val rootNode = bodyStream.progression.getSerializationNode(serializable)
        rootNode.writeBytes(bodyStream)
        pool.writePool(poolStream)
        val v = poolStream.put(bodyStream.array(), 0, bodyStream.position())
            .array().take(poolStream.position())
        v
    }

    override def isSameSignature(bytes: Array[Byte]): Boolean = bytes.startsWith(signature)

    override def deserialize(bytes: Array[Byte]): Any = {
        val in = new DefaultDeserializationInputStream(ByteBuffer.wrap(bytes), context)
        in.progression.getNextDeserializationNode
                .deserialize(in)
    }

    override def deserializeAll(bytes: Array[Byte]): Array[Any] = {
        deserialize(bytes).asInstanceOf[Array[Any]]
    }

    def initNetwork(network: Network): Unit = {
        if (this.network != null)
            throw new IllegalStateException("Network already initialised.")
        this.network = network
        context.addPersistence(new PuppetWrapperPersistor(network))
    }

}
