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
import fr.linkit.core.connection.packet.serialization.tree._
import fr.linkit.core.local.utils.NumberSerializer
import fr.linkit.core.local.utils.ScalaUtils.toPresentableString

object PrimitiveNode {

    val ByteFlag     : Byte = 1
    val ShortFlag    : Byte = 2
    val IntFlag      : Byte = 4
    val LongFlag     : Byte = 8
    val FloatFlag    : Byte = 16
    val DoubleFlag   : Byte = 32
    val CharFlag     : Byte = 64
    val BooleanFlag  : Byte = 127
    val PrimitiveFlag: Byte = -128

    private val TypeFlags = Array(ByteFlag, ShortFlag, LongFlag, FloatFlag, DoubleFlag, CharFlag, BooleanFlag)

    private val OtherWrapperClasses: Array[Class[_]] = Array(classOf[Character], classOf[java.lang.Boolean])

    def apply[T <: AnyVal]: NodeFactory[T] = new NodeFactory[T] {
        override def canHandle(clazz: Class[_]): Boolean = {
            clazz.isPrimitive || (classOf[Number].isAssignableFrom(clazz) && clazz.getPackageName == "java.lang") || OtherWrapperClasses.contains(clazz)
        }

        override def canHandle(info: ByteSeqInfo): Boolean = !info.isClassDefined && info.bytes.nonEmpty && {
            TypeFlags.exists(flag => (info.bytes(0) & flag) == flag)
        }

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
            val (bytes, flag) = t match {
                case i: Int     => (NumberSerializer.serializeNumber(i, true), IntFlag)
                case b: Byte    => (NumberSerializer.serializeNumber(b, true), ByteFlag)
                case s: Short   => (NumberSerializer.serializeNumber(s, true), ShortFlag)
                case l: Long    => (NumberSerializer.serializeNumber(l, true), LongFlag)
                case d: Double  => (NumberSerializer.serializeNumber(java.lang.Double.doubleToLongBits(d), true), DoubleFlag)
                case f: Float   => (NumberSerializer.serializeNumber(java.lang.Float.floatToIntBits(f), true), FloatFlag)
                case b: Boolean => ((1: Byte) /\ (if (b) 1 else 0).toByte, BooleanFlag)
                case c: Char    => (NumberSerializer.serializeNumber(c.toInt, true), CharFlag)
            }
            /*if (!putTypeHint)
                return bytes //removing first type hint*/
            (PrimitiveFlag + flag).toByte /\ bytes
        }
    }

    class PrimitiveDeserialNode[T <: AnyVal](bytes: Array[Byte], override val parent: DeserialNode[_]) extends DeserialNode[T] {

        override def deserialize(): T = {
            println(s"Deserializing primitive of bytes ${toPresentableString(bytes)}")
            NumberSerializer.deserializeFlaggedNumber(bytes, 1)._1
        }
    }

}
