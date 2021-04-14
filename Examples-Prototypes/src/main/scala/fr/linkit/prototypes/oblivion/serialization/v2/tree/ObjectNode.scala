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

package fr.linkit.prototypes.oblivion.serialization.v2.tree

import fr.linkit.core.local.mapping.ClassMappings
import fr.linkit.core.local.utils.NumberSerializer
import fr.linkit.core.local.utils.ScalaUtils.toPresentableString
import sun.misc.Unsafe

import java.lang.reflect.Field
import scala.reflect.{ClassTag, classTag}

object ObjectNode {

    val Constraints   : Array[Class[_] => Boolean] = Array(_.isPrimitive, _.isArray, _.isEnum, _ == classOf[String])
    val NullObjectFlag: Byte                       = -76

    def apply: NodeFactory[Serializable] = new NodeFactory[Serializable] {
        override def canHandle(clazz: Class[_]): Boolean = {
            !Constraints.exists(_ (clazz))
        }

        override def canHandle(bytes: Array[Byte]): Boolean = {
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
            if (t == null)
                return Array(NullObjectFlag)
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
            val desc       = tree.getDesc(objectType)

            val sign     = LengthSign.from(desc.signItemCount, bytes, bytes.length, 4)
            val instance = TheUnsafe.allocateInstance(desc.clazz)

            var i = 0
            for (childBytes <- sign.childrenBytes) {
                println(s"For object field number $i: ")
                println(s"Field bytes = ${toPresentableString(childBytes)}")
                val node = tree.getDeserialNodeFor(childBytes, this)
                println(s"node = ${node}")
                val field = desc.serializableFields(i)
                println(s"field = ${field}")
                val fieldValue = node.deserialize()
                println(s"Deserialized value : $fieldValue")
                setValue(instance, field, fieldValue)

                i += 1
            }
            println(s"Instance = $instance")
            instance.asInstanceOf[Serializable]
        }

        private def setValue(instance: AnyRef, field: Field, value: Any): Unit = {
            val fieldOffset = TheUnsafe.objectFieldOffset(field)

            import java.lang

            println(s"value.getClass = ${value.getClass}")

            def isFieldOfType[T: ClassTag]: Boolean = field.getType == classTag[T].runtimeClass

            val action: (AnyRef, Long) => Unit = value match {
                case i: Integer if isFieldOfType[Int]          => TheUnsafe.putInt(_, _, i)
                case b: lang.Byte if isFieldOfType[Byte]       => TheUnsafe.putByte(_, _, b)
                case s: lang.Short if isFieldOfType[Short]     => TheUnsafe.putShort(_, _, s)
                case l: lang.Long if isFieldOfType[Long]       => TheUnsafe.putLong(_, _, l)
                case d: lang.Double if isFieldOfType[Double]   => TheUnsafe.putDouble(_, _, d)
                case f: lang.Float if isFieldOfType[Float]     => TheUnsafe.putFloat(_, _, f)
                case b: lang.Boolean if isFieldOfType[Boolean] => TheUnsafe.putBoolean(_, _, b)
                case c: Character if isFieldOfType[Char]       => TheUnsafe.putChar(_, _, c)
                case obj                                       => TheUnsafe.putObject(_, _, obj)
            }
            action(instance, fieldOffset)
        }
    }

}
