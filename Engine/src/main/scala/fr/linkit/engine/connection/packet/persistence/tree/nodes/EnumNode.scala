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

package fr.linkit.engine.connection.packet.persistence.tree.nodes

import fr.linkit.api.connection.packet.persistence.tree._
import fr.linkit.engine.local.utils.NumberSerializer


object EnumNode {

    def apply[E <: Enum[E]]: NodeFactory[E] = new NodeFactory[E] {
        override def canHandle(clazz: Class[_]): Boolean = clazz.isEnum

        override def canHandle(bytes: ByteSeq): Boolean = {
            bytes.classExists(_.isEnum)
        }

        override def newNode(finder: NodeFinder, profile: ClassProfile[E]): SerialNode[E] = {
            new EnumSerialNode[E](profile)
        }

        override def newNode(finder: NodeFinder, seq: ByteSeq): DeserialNode[E] = {
            new EnumDeserialNode[E](finder.getClassProfile(seq.getClassOfSeq), seq)
        }
    }

    class EnumSerialNode[E <: Enum[E]](profile: ClassProfile[E]) extends SerialNode[E] {

        override def serialize(t: E, putTypeHint: Boolean): Array[Byte] = {
            profile.applyAllSerialProcedures(t)
            //println(s"Serializing enum ${t}")
            val name     = t.name()
            val enumType = NumberSerializer.serializeInt(t.getClass.getName.hashCode)
            val result   = enumType ++ name.getBytes
            //println(s"Result = ${toPresentableString(result)} (type: ${toPresentableString(enumType)}, name: $name)")
            result
        }
    }

    class EnumDeserialNode[E <: Enum[E]](profile: ClassProfile[E], bytes: ByteSeq) extends DeserialNode[E] {

        override def deserialize(): E = {
            //println(s"Deserializing enum ${toPresentableString(bytes)}")
            val enumType = bytes.getClassOfSeq
            val name     = new String(bytes.array.drop(4))
            //println(s"Name = $name")
            val enum = Enum.valueOf(enumType.asInstanceOf[Class[E]], name)
            profile.applyAllDeserialProcedures(enum)
            enum
        }
    }

}
