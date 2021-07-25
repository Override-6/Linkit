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

package fr.linkit.engine.connection.packet.persistence.v3.deserialisation

import fr.linkit.api.connection.packet.persistence.v3.PersistenceContext
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.{DeserializerNode, ObjectDeserializerNode}
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.{DeserialisationInputStream, DeserialisationProgression}
import fr.linkit.engine.connection.packet.persistence.v3.ArraySign
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.{NonObjectDeserializerNode, SizedDeserializerNode}
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.SerializerNodeFlags._
import fr.linkit.engine.local.mapping.{ClassMappings, ClassNotMappedException}
import fr.linkit.engine.local.utils.NumberSerializer

class DefaultDeserialisationProgression(in: DeserialisationInputStream, context: PersistenceContext) extends DeserialisationProgression {

    private var poolObject: Array[Any] = _

    initPool()

    override def getNextDeserializationNode: DeserializerNode = {
        val buff = in.buff
        val pos  = buff.position()
        buff.get(pos) match {
            case b if b >= ByteFlag && b <= BooleanFlag => NonObjectDeserializerNode(_.readPrimitive())
            case StringFlag                             => NonObjectDeserializerNode(_.readString())
            case ArrayFlag                              => NonObjectDeserializerNode(_.readArray())
            case HeadedObjectFlag                       =>
                buff.position(buff.position() + 1)
                getHeaderObjectNode(NumberSerializer.deserializeFlaggedNumber[Int](in))
            case _                                      =>
                //buff.position(buff.position() - 1) //for object, no flag is set, the first byte is a member of the object type int code, so we need to make a rollback in order to integrate the first byte.
                val classCode   = buff.getInt
                val objectClass = ClassMappings.getClass(classCode)
                if (objectClass == null)
                    throw new ClassNotMappedException(s"classCode $classCode is not mapped.")
                if (objectClass.isEnum)
                    NonObjectDeserializerNode(_.readEnum())
                else context.getPersistence(objectClass)
                        .getDeserialNode(context.getDescription(objectClass), context, this)
        }
    }

    private def fillPool(nodes: Seq[DeserializerNode], postInit: Boolean): Unit = {
        def carefulDeserial(i: Int, node: ObjectDeserializerNode): Unit = {
            poolObject(i) = node.getRef
            if (postInit)
                node.deserialize(in)
        }

        for (i <- nodes.indices) {
            nodes(i) match {
                case node: ObjectDeserializerNode                        =>
                    carefulDeserial(i, node)
                case SizedDeserializerNode(node: ObjectDeserializerNode) =>
                    carefulDeserial(i, node)
                case node                                                =>
                    if (!postInit)
                        poolObject(i) = node.deserialize(in)
            }
        }
    }

    override def getHeaderObjectNode(place: Int): DeserializerNode = {
        val obj = poolObject(place)
        if (obj == null) {
            throw new NullPointerException("Unexpected null item in poolObject")
        }
        _ => obj
    }

    private def initPool(): Unit = {
        val length = NumberSerializer.deserializeFlaggedNumber[Int](in)
        val count  = NumberSerializer.deserializeFlaggedNumber[Int](in)
        poolObject = new Array[Any](count + 1)
        in.limit(length + in.position())
        ArraySign.in(count, this, in).deserializeRef(poolObject)(nodes => {
            fillPool(nodes, false)
            fillPool(nodes, true)
        }).deserialize(in)
        in.limit(in.capacity())
    }

}