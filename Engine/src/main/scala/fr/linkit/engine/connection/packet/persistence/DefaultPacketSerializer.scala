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
import fr.linkit.engine.connection.packet.persistence.v3.DefaultPacketPersistenceContext
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.DefaultDeserializationInputStream
import fr.linkit.engine.connection.packet.persistence.v3.persistor.PuppetWrapperPersistor
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.{DefaultSerialisationObjectPool, DefaultSerialisationOutputStream}

import java.nio.ByteBuffer

class DefaultPacketSerializer extends Serializer {

    val context = new DefaultPacketPersistenceContext()
    override val signature: Array[Byte] = Array(4)

    private var network: Network = _

    override def serialize(objects: Array[AnyRef], buff: ByteBuffer, withSignature: Boolean): Unit = {
        val pool       = new DefaultSerialisationObjectPool()
        val poolStream = new DefaultSerialisationOutputStream(buff, pool, context)
        val bodyStream = new DefaultSerialisationOutputStream(ByteBuffer.allocateDirect(buff.capacity() / 2), pool, context)
        objects
                .map(obj => bodyStream.progression.getSerializationNode(obj))
                .foreach(_.writeBytes(bodyStream))
        pool.writePool(poolStream)
        bodyStream.flip()
        poolStream.put(bodyStream)
    }

    override def isSameSignature(buff: ByteBuffer): Boolean = {
        val header = new Array[Byte](signature.length)
        buff.get(0, header)
        header sameElements signature
    }

    override def deserialize(buff: ByteBuffer)(f: Any => Unit): Unit = {
        val in = new DefaultDeserializationInputStream(buff, context)
        val lim = buff.limit()
        while (lim > buff.position() && buff.get(buff.position()) != 0) {
            val obj = in.progression.getNextDeserializationNode
                    .deserialize(in)
            buff.limit(lim)
            f(obj)
        }
    }

    def initNetwork(network: Network): Unit = {
        if (this.network != null)
            throw new IllegalStateException("Network already initialised.")
        this.network = network
        context.addPersistence(new PuppetWrapperPersistor(network))
    }

}
