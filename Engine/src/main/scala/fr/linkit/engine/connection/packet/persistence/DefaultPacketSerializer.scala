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
import fr.linkit.api.connection.packet.PacketCoordinates
import fr.linkit.api.connection.packet.persistence.PacketSerializer
import fr.linkit.api.connection.packet.persistence.PacketSerializer.PacketDeserial
import fr.linkit.engine.connection.cache.obj.generation.DefaultObjectWrapperClassCenter
import fr.linkit.engine.connection.packet.persistence.DefaultPacketSerializer.{AnyPacketCoordinatesFlag, NoPacketCoordinates, NoPacketCoordinatesFlag}
import fr.linkit.engine.connection.packet.persistence.v3.DefaultPacketPersistenceContext
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.{DefaultDeserializationInputStream, DefaultDeserializationObjectPool, EmptyDeserializationObjectPool}
import fr.linkit.engine.connection.packet.persistence.v3.persistor.{DefaultObjectPersistor, SynchronizedObjectPersistor}
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.{DefaultSerialisationOutputStream, DefaultSerializationObjectPool, DefaultSerializationProgression, FakeSerializationObjectPool}

import java.nio.ByteBuffer

class DefaultPacketSerializer(center: DefaultObjectWrapperClassCenter) extends PacketSerializer {

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
                val fakeOut     = new DefaultSerialisationOutputStream(buff, coordinates, FakeSerializationObjectPool, context)
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

    override def deserializePacket(buff: ByteBuffer): PacketDeserial = {
        val lim                            = buff.limit()
        val coordinates = {
            buff.get match {
                case NoPacketCoordinatesFlag  => NoPacketCoordinates
                case AnyPacketCoordinatesFlag =>
                    buff.position(buff.position() + 1)
                    val in = new DefaultDeserializationInputStream(buff, context, NoPacketCoordinates, _ => EmptyDeserializationObjectPool)
                    val coords = DefaultObjectPersistor
                            .getDeserialNode(context.getDescription(in.readClass()), context, in.progression)
                            .deserialize(in)
                            .asInstanceOf[PacketCoordinates]
                    in.progression.concludeDeserialization()
                    coords
            }
        }
        buff.limit(lim)
        deserialize(buff, coordinates)
    }

    override def deserialize(buff: ByteBuffer)(f: Any => Unit): Unit = {
        deserializePacket(buff).forEachObjects(f)
    }

    override def isSameSignature(buff: ByteBuffer): Boolean = {
        val header = new Array[Byte](signature.length)
        buff.get(0, header)
        header sameElements signature
    }

    private def deserialize(buff: ByteBuffer, coordinates: PacketCoordinates): PacketDeserial = {
        val in  = new DefaultDeserializationInputStream(buff, context, coordinates, in => new DefaultDeserializationObjectPool(in, center))
        val lim = buff.limit()
        new PacketDeserial {
            var concluded = false

            override def getCoordinates: PacketCoordinates = coordinates

            override def forEachObjects(f: Any => Unit): Unit = {
                if (concluded)
                    throw new IllegalStateException("Objects have already been deserialized.")
                in.initPool()
                while (lim > buff.position() && buff.get(buff.position()) != 0) {
                    val obj = in.progression.getNextDeserializationNode
                            .deserialize(in)
                    buff.limit(lim)
                    f(obj)
                }
                concluded = true
                in.progression.concludeDeserialization()
            }
        }
    }

    def initNetwork(network: Network): Unit = {
        if (this.network != null)
            throw new IllegalStateException("Network already initialised.")
        this.network = network
        context.putPersistor(new SynchronizedObjectPersistor(network))
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
