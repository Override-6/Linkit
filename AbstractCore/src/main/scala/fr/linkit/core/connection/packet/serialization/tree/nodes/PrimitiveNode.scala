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

import fr.linkit.core.connection.packet.serialization.tree.NodeFinder.MegaByte
import fr.linkit.core.connection.packet.serialization.tree.{DeserialNode, NodeFactory, NodeFinder, SerialNode, SerializableClassDescription}
import fr.linkit.core.local.utils.NumberSerializer
import fr.linkit.core.local.utils.ScalaUtils.toPresentableString

object PrimitiveNode {

    val PrimitiveFlag: Byte = -121

    private val OtherWrapperClasses: Array[Class[_]] = Array(classOf[Character], classOf[java.lang.Boolean])

    def apply[T <: AnyVal]: NodeFactory[T] = new NodeFactory[T] {
        override def canHandle(clazz: Class[_]): Boolean = {
            clazz.isPrimitive || (classOf[Number].isAssignableFrom(clazz) && clazz.getPackageName == "java.lang") || OtherWrapperClasses.contains(clazz)
        }

        override def canHandle(bytes: Array[Byte]): Boolean = bytes.nonEmpty && bytes(0) == PrimitiveFlag

        override def newNode(finder: NodeFinder, desc: SerializableClassDescription, parent: SerialNode[_]): SerialNode[T] = {
            new PrimitiveSerialNode[T](parent)
        }

        override def newNode(finder: NodeFinder, bytes: Array[Byte], parent: DeserialNode[_]): DeserialNode[T] = {
            new PrimitiveDeserialNode[T](bytes, parent)
        }
    }

    class PrimitiveSerialNode[T <: AnyVal](override val parent: SerialNode[_]) extends SerialNode[T] {

        override def serialize(t: T, putTypeHint: Boolean): Array[Byte] = {
            println(s"Serializing primitive ${t}")
            val bytes = t match {
                case i: Int     => NumberSerializer.serializeNumber(i, true)
                case b: Byte    => NumberSerializer.serializeNumber(b, true)
                case s: Short   => NumberSerializer.serializeNumber(s, true)
                case l: Long    => NumberSerializer.serializeNumber(l, true)
                case d: Double  => NumberSerializer.serializeNumber(java.lang.Double.doubleToLongBits(d), true)
                case f: Float   => NumberSerializer.serializeNumber(java.lang.Float.floatToIntBits(f), true)
                case b: Boolean => (1: Byte) /\ (if (b) 1 else 0).toByte
                case c: Char    => NumberSerializer.serializeNumber(c.toInt, true)
            }
            if (!putTypeHint)
                return bytes //removing first type hint
            PrimitiveFlag /\ bytes
        }
    }

    class PrimitiveDeserialNode[T <: AnyVal](bytes: Array[Byte], override val parent: DeserialNode[_]) extends DeserialNode[T] {

        override def deserialize(): T = {
            println(s"Deserializing primitive of bytes ${toPresentableString(bytes)}")
            NumberSerializer.deserializeFlaggedNumber(bytes, 1)._1
        }
    }

}
