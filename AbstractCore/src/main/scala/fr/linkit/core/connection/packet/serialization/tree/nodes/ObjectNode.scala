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
import sun.misc.Unsafe

import java.lang.reflect.Field
import scala.util.Try

object ObjectNode {

    val Constraints   : Array[Class[_] => Boolean] = Array(_.isPrimitive, _.isArray, _.isEnum, _ == classOf[String])
    val NullObjectFlag: Byte                       = -76

    def apply: NodeFactory[Serializable] = new NodeFactory[Serializable] {
        override def canHandle(clazz: Class[_]): Boolean = {
            !Constraints.exists(_ (clazz))
        }

        override def canHandle(bytes: Array[Byte]): Boolean = {
            //TODO Optimize NumberSerialier.deserializeInt use
            (bytes.nonEmpty && bytes(0) == NullObjectFlag) || (bytes.length >= 4 && ClassMappings.getClassNameOpt(NumberSerializer.deserializeInt(bytes, 0)).isDefined)
        }

        override def newNode(finder: NodeFinder, desc: SerializableClassDescription, parent: SerialNode[_]): SerialNode[Serializable] = {
            new ObjectSerialNode(parent, desc, finder)
        }

        override def newNode(finder: NodeFinder, bytes: Array[Byte], parent: DeserialNode[_]): DeserialNode[Serializable] = {
            new ObjectDeserialNode(parent, bytes, finder)
        }
    }

    private val TheUnsafe = findUnsafe()

    @throws[IllegalAccessException]
    private def findUnsafe(): Unsafe = {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        for (field <- unsafeClass.getDeclaredFields) {
            if (field.getType eq unsafeClass) {
                field.setAccessible(true)
                return field.get(null).asInstanceOf[Unsafe]
            }
        }
        throw new IllegalStateException("No instance of Unsafe found")
    }

    class ObjectSerialNode(override val parent: SerialNode[_], desc: SerializableClassDescription, tree: NodeFinder) extends SerialNode[Serializable] {

        override def serialize(t: Serializable, putTypeHint: Boolean): Array[Byte] = {
            println(s"Serializing Object ${t}")
            println(s"Object desc = ${desc}")
            if (t == null)
                return Array(NullObjectFlag)
            println(s"t.getClass = ${t.getClass}")
            val children = tree.listNodes(desc, t, this)
            println(s"children = ${children}")

            val classType = desc.classSignature
            println(s"classType = ${toPresentableString(classType)}")
            val sign = LengthSign.of(t, desc, children).toBytes
            println(s"sign = ${toPresentableString(sign)}")
            val bytes = classType ++ sign
            println(s"Result of Object ${t} = ${toPresentableString(bytes)}")
            bytes
        }
    }

    class ObjectDeserialNode(override val parent: DeserialNode[_], bytes: Array[Byte], tree: NodeFinder) extends DeserialNode[Serializable] {

        override def deserialize(): Serializable = {
            if (bytes(0) == NullObjectFlag)
                return null

            println(s"Deserializing object from bytes ${toPresentableString(bytes)}")
            val objectType = ClassMappings.getClass(NumberSerializer.deserializeInt(bytes, 0))
            println(s"objectType = ${objectType}")
            val desc = tree.getDesc(objectType)
            println(s"Object desc = ${desc}")

            val sign     = LengthSign.from(desc.signItemCount, bytes, bytes.length, 4)
            val instance = TheUnsafe.allocateInstance(desc.clazz)

            val fieldValues = for (childBytes <- sign.childrenBytes) yield {
                println(s"Field bytes = ${toPresentableString(childBytes)}")
                val node = tree.getDeserialNodeFor(childBytes, this)
                println(s"node = ${node}")
                node.deserialize()
            }

            desc.foreachDeserializableFields { (i, field) =>
                println(s"For object field number (of object $objectType) $i: ")
                println(s"Deserialized value : ${fieldValues(i)}")
                setValue(instance, field, fieldValues(i))
            }

            println(s"(objectType) = ${objectType}")
            println(s"Instance = $instance")
            instance.asInstanceOf[Serializable]
        }

        private def setValue(instance: AnyRef, field: Field, value: Any): Unit = {
            val fieldOffset = TheUnsafe.objectFieldOffset(field)

            import java.lang

            def convertValue[A <: AnyVal](converter: PrimitiveWrapper => A): A = {
                value match {
                    case n: Number       => converter(new NumberWrapper(n))
                    case b: lang.Boolean => converter(new BooleanNumber(b))
                    case c: Character    => converter(new CharacterNumber(c))
                }
            }

            println(s"value.getClass = ${Try(value.getClass).getOrElse(null)}")
            val action: (AnyRef, Long) => Unit = if (field.getType.isPrimitive) {
                value match {
                    case i: Integer      => TheUnsafe.putInt(_, _, i)
                    case b: lang.Byte    => TheUnsafe.putByte(_, _, b)
                    case s: lang.Short   => TheUnsafe.putShort(_, _, s)
                    case l: lang.Long    => TheUnsafe.putLong(_, _, l)
                    case d: lang.Double  => TheUnsafe.putDouble(_, _, d)
                    case f: lang.Float   => TheUnsafe.putFloat(_, _, f)
                    case b: lang.Boolean => TheUnsafe.putBoolean(_, _, b)
                    case c: Character    => TheUnsafe.putChar(_, _, c)
                }
            } else field.getType match {
                case _: Class[Integer]      => TheUnsafe.putObject(_, _, convertValue(_.intValue))
                case _: Class[lang.Byte]    => TheUnsafe.putObject(_, _, convertValue(_.byteValue))
                case _: Class[lang.Short]   => TheUnsafe.putObject(_, _, convertValue(_.shortValue))
                case _: Class[lang.Long]    => TheUnsafe.putObject(_, _, convertValue(_.longValue))
                case _: Class[lang.Double]  => TheUnsafe.putObject(_, _, convertValue(_.doubleValue))
                case _: Class[lang.Float]   => TheUnsafe.putObject(_, _, convertValue(_.floatValue))
                case _: Class[lang.Boolean] => TheUnsafe.putObject(_, _, convertValue(_.booleanValue))
                case _: Class[Character] => TheUnsafe.putObject(_, _, convertValue(_.shortValue))
            }
            action(instance, fieldOffset)
        }
    }

    private sealed trait PrimitiveWrapper extends Number {

        def booleanValue: Boolean

        def charValue: Char
    }

    private class CharacterNumber(c: Character) extends PrimitiveWrapper {

        override def intValue: Int = c.toInt

        override def longValue: Long = c.toLong

        override def floatValue: Float = c.toFloat

        override def doubleValue: Double = c.toDouble

        override def booleanValue: Boolean = intValue == 1

        override def charValue: Char = c
    }

    private class BooleanNumber(b: java.lang.Boolean) extends PrimitiveWrapper {

        override def intValue: Int = if (b) 1 else 0

        override def longValue: Long = intValue

        override def floatValue: Float = intValue

        override def doubleValue: Double = intValue

        override def booleanValue: Boolean = b

        override def charValue: Char = if (b) 'y' else 'n'
    }
    
    private class NumberWrapper(n: Number) extends PrimitiveWrapper {

        override def booleanValue: Boolean = intValue == 1

        override def charValue: Char = intValue.toChar

        override def intValue: Int = n.intValue

        override def longValue: Long = n.longValue

        override def floatValue: Float = n.floatValue

        override def doubleValue: Double = n.doubleValue
    }

}
