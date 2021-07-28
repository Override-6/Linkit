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

import java.nio.ByteBuffer

import fr.linkit.api.connection.network.Network
import fr.linkit.api.connection.packet.PacketCoordinates
import fr.linkit.api.connection.packet.persistence.PacketSerializer
import fr.linkit.engine.connection.packet.persistence.DefaultPacketSerializer.{AnyPacketCoordinatesFlag, NoPacketCoordinates, NoPacketCoordinatesFlag}
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.{DefaultDeserializationInputStream, DefaultDeserializationObjectPool, EmptyDeserializationObjectPool}
import fr.linkit.engine.connection.packet.persistence.v3.persistor.{DefaultObjectPersistor, PuppetWrapperPersistor}
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.{DefaultSerialisationOutputStream, DefaultSerializationProgression, DefaultSerializationObjectPool, FakeSerializationObjectPool}
import fr.linkit.engine.connection.packet.persistence.v3.{ClassDescription, DefaultPacketPersistenceContext}

class DefaultPacketSerializer extends PacketSerializer {

    val context = new DefaultPacketPersistenceContext()
    override val signature: Array[Byte] = Array(4)

    private var network: Network = _

    override def serialize(objects: Array[AnyRef], buff: ByteBuffer, withSignature: Boolean): Unit = {
        serializePacket(objects, NoPacketCoordinates, buff, withSignature)
    }

    override def serializePacket(objects: Array[AnyRef], coordinates: PacketCoordinates, buff: ByteBuffer, withSignature: Boolean): Unit = {
        val pool       = new DefaultSerializationObjectPool()
        val poolStream = new DefaultSerialisationOutputStream(buff, coordinates, pool, context)
        val bodyStream = new DefaultSerialisationOutputStream(ByteBuffer.allocateDirect(buff.capacity() / 2), coordinates, pool, context)
        coordinates match {
            case NoPacketCoordinates =>
                poolStream.put(NoPacketCoordinatesFlag)
            case other               =>
                poolStream.put(AnyPacketCoordinatesFlag)
                val fakeOut = new DefaultSerialisationOutputStream(buff, coordinates, FakeSerializationObjectPool, context)
                val fakeProgess = new DefaultSerializationProgression(context, FakeSerializationObjectPool, NoPacketCoordinates, fakeOut)
                DefaultObjectPersistor
                    .getSerialNode(other, context.getDescription(other.getClass), context, fakeProgess)
                    .writeBytes(fakeOut)
        }
        objects
            .map(obj => bodyStream.progression.getSerializationNode(obj))
            .foreach(_.writeBytes(bodyStream))
        pool.writePool(poolStream)
        bodyStream.flip()
        poolStream.put(bodyStream)
    }

    override def deserializePacket(buff: ByteBuffer)(g: PacketCoordinates => Unit)(f: Any => Unit): Unit = {
        val lim = buff.limit()
        val coordinates = {
            buff.get match {
                case NoPacketCoordinatesFlag  => NoPacketCoordinates
                case AnyPacketCoordinatesFlag =>
                    buff.position(buff.position() + 1)
                    val in = new DefaultDeserializationInputStream(buff, context, NoPacketCoordinates, _ => EmptyDeserializationObjectPool)
                    DefaultObjectPersistor
                        .getDeserialNode(context.getDescription(in.readClass()), context, in.progression)
                        .deserialize(in)
                        .asInstanceOf[PacketCoordinates]
            }
        }
        buff.limit(lim)
        g(coordinates)
        deserialize(buff, coordinates)(f)
    }

    override def deserialize(buff: ByteBuffer)(f: Any => Unit): Unit = {
        deserializePacket(buff)((_: PacketCoordinates) => _)(f)
    }

    override def isSameSignature(buff: ByteBuffer): Boolean = {
        val header = new Array[Byte](signature.length)
        buff.get(0, header)
        header sameElements signature
    }

    private def deserialize(buff: ByteBuffer, coordinates: PacketCoordinates)(f: Any => Unit): Unit = {
        val in  = new DefaultDeserializationInputStream(buff, context, coordinates, in => new DefaultDeserializationObjectPool(in))
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

object DefaultPacketSerializer {

    private val NoPacketCoordinatesFlag : Byte = -111
    private val AnyPacketCoordinatesFlag: Byte = -112

    object NoPacketCoordinates extends PacketCoordinates {

        override val injectableID: Int    = -1
        override val senderID    : String = null

        override def forallConcernedTargets(action: String => Boolean): Boolean = true
    }

}
