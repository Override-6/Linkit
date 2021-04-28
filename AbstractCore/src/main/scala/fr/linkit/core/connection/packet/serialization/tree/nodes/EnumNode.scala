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
import fr.linkit.core.local.utils.ScalaUtils.toPresentableString

object EnumNode {

    def apply[E <: Enum[E]]: NodeFactory[E] = new NodeFactory[E] {
        override def canHandle(clazz: Class[_]): Boolean = clazz.isEnum

        override def canHandle(bytes: ByteSeqInfo): Boolean = {
            bytes.classExists(_.isEnum)
        }

        override def newNode(finder: NodeFinder, desc: SerializableClassDescription, parent: SerialNode[_]): SerialNode[E] = {
            new EnumSerialNode[E](parent)
        }

        override def newNode(finder: NodeFinder, bytes: Array[Byte], parent: DeserialNode[_]): DeserialNode[E] = {
            new EnumDeserialNode[E](parent, bytes)
        }
    }

    class EnumSerialNode[E <: Enum[E]](override val parent: SerialNode[_]) extends SerialNode[E] {

        override def serialize(t: E, putTypeHint: Boolean): Array[Byte] = {
            println(s"Serializing enum ${t}")
            val name     = t.name()
            val enumType = NumberSerializer.serializeInt(t.getClass.getName.hashCode)
            val result   = enumType ++ name.getBytes
            println(s"Result = ${toPresentableString(result)} (type: ${toPresentableString(enumType)}, name: $name)")
            result
        }
    }

    class EnumDeserialNode[E <: Enum[E]](override val parent: DeserialNode[_], bytes: Array[Byte]) extends DeserialNode[E] {

        override def deserialize(): E = {
            println(s"Deserializing enum ${toPresentableString(bytes)}")
            val enumType = ClassMappings.getClass(NumberSerializer.deserializeInt(bytes, 0))
            val name     = new String(bytes.drop(4))
            println(s"Name = $name")
            Enum.valueOf(enumType.asInstanceOf[Class[E]], name)
        }
    }

}
