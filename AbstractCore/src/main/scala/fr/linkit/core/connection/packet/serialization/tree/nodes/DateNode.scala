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

package fr.linkit.core.connection.packet.serialization.tree.nodes

import fr.linkit.core.connection.packet.serialization.tree._
import fr.linkit.core.local.mapping.ClassMappings
import fr.linkit.core.local.utils.NumberSerializer

import java.util.Date

object DateNode extends NodeFactory[Date] {

    override def canHandle(clazz: Class[_]): Boolean = classOf[Date].isAssignableFrom(clazz)

    override def canHandle(info: ByteSeqInfo): Boolean = info.classExists(canHandle)

    override def newNode(finder: NodeFinder, desc: SerializableClassDescription, parent: SerialNode[_]): SerialNode[Date] = {
        new DateSerialNode(parent)
    }

    override def newNode(finder: NodeFinder, bytes: Array[Byte], parent: DeserialNode[_]): DeserialNode[Date] = {
        new DateDeserialNode(bytes, parent)
    }

    private class DateSerialNode(override val parent: SerialNode[_]) extends SerialNode[Date] {

        override def serialize(t: Date, putTypeHint: Boolean): Array[Byte] = {
            val i = t.getTime
            println(s"long = ${i}")
            println(s"classType = ${t.getClass.getName}")
            NumberSerializer.serializeInt(t.getClass.getName.hashCode) ++ NumberSerializer.serializeLong(i)
        }
    }

    private class DateDeserialNode(bytes: Array[Byte], override val parent: DeserialNode[_]) extends DeserialNode[Date] {

        override def deserialize(): Date = {
            val classType = NumberSerializer.deserializeInt(bytes, 0)
            val long = NumberSerializer.deserializeLong(bytes, 4)
            println(s"long = ${long}")
            println(s"classType = ${classType}")
            val clazz = ClassMappings.getClass(classType)
            clazz.getDeclaredConstructor(classOf[Long])
                    .newInstance(NumberSerializer.deserializeLong(bytes, 4))
                    .asInstanceOf[Date]
        }
    }

}
