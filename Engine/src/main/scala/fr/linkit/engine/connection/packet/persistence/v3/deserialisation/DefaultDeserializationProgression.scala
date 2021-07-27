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

import fr.linkit.api.connection.packet.PacketCoordinates
import fr.linkit.api.connection.packet.persistence.v3.PacketPersistenceContext
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.DeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.{DeserializationInputStream, DeserializationObjectPool, DeserializationProgression}
import fr.linkit.engine.connection.packet.persistence.MalFormedPacketException
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.{NonObjectDeserializerNode, RawObjectNode}
import fr.linkit.engine.connection.packet.persistence.v3.helper.ArrayPersistence
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.SerializerNodeFlags._
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.NullInstanceNode.NoneFlag
import fr.linkit.engine.local.mapping.{ClassMappings, ClassNotMappedException}
import fr.linkit.engine.local.utils.NumberSerializer

class DefaultDeserializationProgression(in: DeserializationInputStream,
                                        override val pool: DeserializationObjectPool,
                                        context: PacketPersistenceContext,
                                        override val coordinates: PacketCoordinates) extends DeserializationProgression {

    override def getNextDeserializationNode: DeserializerNode = {
        val buff     = in.buff
        val startPos = buff.position()
        buff.get match {
            case b if b >= ByteFlag && b <= BooleanFlag =>
                buff.position(startPos)
                NonObjectDeserializerNode(_.readPrimitive())
            case StringFlag                             =>
                buff.position(startPos)
                NonObjectDeserializerNode(_.readString())
            case ArrayFlag                              => ArrayPersistence.deserialize(in)
            case HeadedValueFlag                        => getHeaderValueNode(NumberSerializer.deserializeFlaggedNumber[Int](in))
            case NullFlag                               => RawObjectNode(if (buff.limit() > buff.position() && buff.get(buff.position()) == NoneFlag) None else null)
            case ObjectFlag                             =>
                if (buff.get(startPos + 1) == HeadedValueFlag) {
                    in.position(startPos + 2)
                    return getHeaderValueNode(NumberSerializer.deserializeFlaggedNumber[Int](in))
                }
                val classCode   = buff.getInt
                val objectClass = ClassMappings.getClass(classCode)
                if (objectClass == null)
                    throw new ClassNotMappedException(s"classCode $classCode is not mapped.")
                if (objectClass.isEnum)
                    NonObjectDeserializerNode(_.readEnum(hint = objectClass))
                else {
                    context.getPersistenceForDeserialisation(objectClass)
                            .getDeserialNode(context.getDescription(objectClass), context, this)
                }
            case flag                                   => throw new MalFormedPacketException(buff, s"Unknown flag '$flag' at start of node expression.")
        }
    }

}