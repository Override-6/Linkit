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

package fr.linkit.core.connection.packet.serialization.v2.tree

import fr.linkit.core.connection.packet.serialization.v2.tree.ClassTree.MegaByte
import fr.linkit.core.local.utils.{NumberSerializer, ScalaUtils}

object PrimitiveNode {

    val CharFlag   : Byte = -100
    val IntFlag    : Byte = -99
    val FloatFlag  : Byte = -98
    val LongFlag   : Byte = -97
    val ShortFlag  : Byte = -96
    val ByteFlag   : Byte = -96
    val DoubleFlag : Byte = -96
    val BooleanFlag: Byte = -96

    val Flags: Array[Byte] = Array(CharFlag, IntFlag, FloatFlag, LongFlag, ShortFlag, ByteFlag, DoubleFlag, BooleanFlag)

    private val OtherWrapperClasses: Array[Class[_]] = Array(classOf[Character], classOf[Boolean])

    def apply[T <: AnyVal]: NodeFactory[T] = new NodeFactory[T] {
        override def canHandle(clazz: Class[_]): Boolean = {
            clazz.isPrimitive || (classOf[Number].isAssignableFrom(clazz) && clazz.getPackageName == "java.lang") || OtherWrapperClasses.contains(clazz)
        }

        override def canHandle(bytes: Array[Byte]): Boolean = bytes.nonEmpty && Flags.contains(bytes(0))

        override def newNode(tree: ClassTree, desc: SerializableClassDescription, parent: SerialNode[_]): SerialNode[T] = {
            new PrimitiveSerialNode[T](parent)
        }

        override def newNode(tree: ClassTree, bytes: Array[Byte], parent: DeserialNode[_]): DeserialNode[T] = {
            new PrimitiveDeserialNode[T](bytes, parent)
        }
    }

    class PrimitiveSerialNode[T <: AnyVal](override val parent: SerialNode[_]) extends SerialNode[T] {

        override def serialize(t: T, putTypeHint: Boolean): Array[Byte] = {
            println(s"Serializing primitive ${t}")
            val bytes = t match {
                case i: Int     => IntFlag /\ NumberSerializer.serializeNumber(i)
                case b: Byte    => ByteFlag /\ NumberSerializer.serializeNumber(b)
                case s: Short   => ShortFlag /\ NumberSerializer.serializeNumber(s)
                case l: Long    => LongFlag /\ NumberSerializer.serializeNumber(l)
                case d: Double  => DoubleFlag /\ NumberSerializer.serializeNumber(java.lang.Double.doubleToLongBits(d))
                case f: Float   => FloatFlag /\ NumberSerializer.serializeNumber(java.lang.Float.floatToIntBits(f))
                case b: Boolean => BooleanFlag /\ (if (b) 1 else 0).toByte
                case c: Char    => CharFlag /\ NumberSerializer.serializeNumber(c.toInt)
            }
            if (!putTypeHint)
                return bytes.drop(1) //removing first type hint
            bytes
        }
    }

    class PrimitiveDeserialNode[T <: AnyVal](bytes: Array[Byte], override val parent: DeserialNode[_]) extends DeserialNode[T] {

        override def deserialize(): T = {
            println(s"Deserializing primitive of bytes ${ScalaUtils.toPresentableString(bytes)}")
            val flag = bytes(0)
            println(s"flag = ${flag}")
            val v = flag match {
                case ByteFlag    => bytes(1)
                case IntFlag     => NumberSerializer.deserializeFlaggedNumber[Int](bytes, 0)._1
                case ShortFlag   => NumberSerializer.deserializeFlaggedNumber[Short](bytes, 0)._1
                case LongFlag    => NumberSerializer.deserializeFlaggedNumber[Long](bytes, 0)._1
                case DoubleFlag  => java.lang.Double.longBitsToDouble(NumberSerializer.deserializeFlaggedNumber[Long](bytes, 0)._1)
                case FloatFlag   => java.lang.Float.intBitsToFloat(NumberSerializer.deserializeFlaggedNumber[Int](bytes, 0)._1)
                case BooleanFlag => NumberSerializer.deserializeFlaggedNumber[Byte](bytes, 0)._1
                case CharFlag    => NumberSerializer.deserializeFlaggedNumber[Int](bytes, 0)._1.toChar
            }
            v.asInstanceOf[T]
        }
    }

}

