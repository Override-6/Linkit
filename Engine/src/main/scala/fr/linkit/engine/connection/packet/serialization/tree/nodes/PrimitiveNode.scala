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

package fr.linkit.engine.connection.packet.serialization.tree.nodes

import fr.linkit.engine.connection.packet.serialization.tree.SerialContext.{ClassProfile, MegaByte}
import fr.linkit.engine.connection.packet.serialization.tree._
import fr.linkit.engine.local.utils.{NumberSerializer, ScalaUtils}

object PrimitiveNode {

    val ByteFlag   : Byte = 1
    val ShortFlag  : Byte = 2
    val IntFlag    : Byte = 4
    val LongFlag   : Byte = 8
    val FloatFlag  : Byte = 16
    val DoubleFlag : Byte = 32
    val CharFlag   : Byte = 64
    val BooleanFlag: Byte = 127

    private val TypeFlags = Array(ByteFlag, ShortFlag, IntFlag, LongFlag, FloatFlag, DoubleFlag, CharFlag, BooleanFlag)

    private val OtherWrapperClasses: Array[Class[_]] = Array(classOf[Character], classOf[java.lang.Boolean])

    def apply: NodeFactory[AnyVal] = new NodeFactory[AnyVal] {
        override def canHandle(clazz: Class[_]): Boolean = {
            clazz.isPrimitive || (classOf[Number].isAssignableFrom(clazz) && clazz.getPackageName == "java.lang") || OtherWrapperClasses.contains(clazz)
        }

        override def canHandle(info: ByteSeq): Boolean = !info.isClassDefined && info.array.nonEmpty && {
            //println(s"info.bytes = ${info.array.mkString("Array(", ", ", ")")}")
            TypeFlags.exists(info.sameFlag)
        }

        override def newNode(context: SerialContext, profile: ClassProfile[AnyVal]): SerialNode[AnyVal] = {
            new PrimitiveSerialNode(profile)
        }

        override def newNode(context: SerialContext, bytes: ByteSeq): DeserialNode[AnyVal] = {
            new PrimitiveDeserialNode(bytes, context)
        }
    }

    class PrimitiveSerialNode[T <: AnyVal](profile: ClassProfile[AnyVal]) extends SerialNode[T] {

        override def serialize(t: T, putTypeHint: Boolean): Array[Byte] = {
            //println(s"Serializing primitive ${t}")
            profile.applyAllSerialProcedures(t)
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
            flag /\ bytes
        }
    }

    class PrimitiveDeserialNode(bytes: Array[Byte], context: SerialContext) extends DeserialNode[AnyVal] {

        override def deserialize(): AnyVal = {
            //println(s"Deserializing primitive number from bytes ${ScalaUtils.toPresentableString(bytes)}")
            //println(s"raw bytes = ${bytes.mkString("Array(", ", ", ")")}")
            import NumberSerializer.{convertValue, deserializeFlaggedNumber}
            val value  = deserializeFlaggedNumber[AnyVal](bytes, 1)._1
            //println(s"value = ${value}")
            val result = bytes(0) match {
                case IntFlag     => convertValue(value, _.intValue)
                case ByteFlag    => convertValue(value, _.byteValue)
                case ShortFlag   => convertValue(value, _.shortValue)
                case FloatFlag   => convertValue(value, _.floatValue)
                case LongFlag    => convertValue(value, _.longValue)
                case DoubleFlag  => convertValue(value, _.doubleValue)
                case CharFlag    => convertValue(value, _.charValue)
                case BooleanFlag => convertValue(value, _.booleanValue)
            }
            //println(s"result = ${result}")
            //println(s"result.getClass = ${result.getClass}")
            context.getClassProfile[AnyVal](result.getClass.asInstanceOf[Class[_ <: AnyVal]]).applyAllDeserialProcedures(result)
            result
        }
    }

}
