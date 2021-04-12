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

import fr.linkit.core.local.mapping.ClassMappings
import fr.linkit.core.local.utils.NumberSerializer
import sun.misc.Unsafe

import java.lang.reflect.Field

object ObjectNode {

    val Constraints: Array[Class[_] => Boolean] = Array(_.isPrimitive, _.isArray, _.isEnum)

    def apply: NodeFactory[Serializable] = new NodeFactory[Serializable] {
        override def canHandle(clazz: Class[_]): Boolean = {
            println(s"for class : ${clazz}")
            val verdict = !Constraints.exists(_ (clazz))
            println(s"verdict = ${verdict}")
            verdict
        }

        override def canHandle(bytes: Array[Byte]): Boolean = ClassMappings.getClassNameOpt(NumberSerializer.deserializeInt(bytes, 0)).isDefined

        override def newNode(tree: ClassTree, desc: SerializableClassDescription, parent: SerialNode[_]): SerialNode[Serializable] = {
            new ObjectSerialNode(parent, desc, tree)
        }

        override def newNode(tree: ClassTree, bytes: Array[Byte], parent: DeserialNode[_]): DeserialNode[Serializable] = {
            new ObjectDeserialNode(parent, bytes, tree)
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

    class ObjectSerialNode(override val parent: SerialNode[_], desc: SerializableClassDescription, tree: ClassTree) extends SerialNode[Serializable] {

        override def serialize(t: Serializable, putTypeHint: Boolean): Array[Byte] = {
            val children = tree.listNodes(desc, t, this)
            println(s"Serializing ${t}")
            println(s"children = ${children}")

            t match {
                case s: String => s.getBytes()
                case _         =>
                    val classType = desc.classSignature
                    val sign      = LengthSign.of(t, desc, children)
                    classType ++ sign.toBytes
            }
        }
    }

    class ObjectDeserialNode(override val parent: DeserialNode[_], bytes: Array[Byte], tree: ClassTree) extends DeserialNode[Serializable] {

        override def deserialize(): Serializable = {
            val objectType = ClassMappings.getClass(NumberSerializer.deserializeInt(bytes, 0))
            val desc       = tree.getDesc(objectType)

            val sign     = LengthSign.from(desc.signItemCount, bytes, bytes.length - 4, 4)
            val instance = TheUnsafe.allocateInstance(desc.clazz)

            var i = 0
            for (childBytes <- sign.childrenBytes) {
                val node       = tree.getNodeFor(childBytes, this)
                val field      = desc.serializableFields(i)
                val fieldValue = node.deserialize()
                setValue(instance, field, fieldValue)

                i += 1
            }
            instance.asInstanceOf[Serializable]
        }

        private def setValue(instance: AnyRef, field: Field, value: Any): Unit = {
            val fieldOffset = TheUnsafe.objectFieldOffset(field)

            val action: (Any, Long) => Unit = value match {
                case i: Int     => TheUnsafe.putInt(_, _, i)
                case b: Byte    => TheUnsafe.putByte(_, _, b)
                case s: Short   => TheUnsafe.putShort(_, _, s)
                case l: Long    => TheUnsafe.putLong(_, _, l)
                case d: Double  => TheUnsafe.putDouble(_, _, d)
                case f: Float   => TheUnsafe.putFloat(_, _, f)
                case b: Boolean => TheUnsafe.putBoolean(_, _, b)
                case c: Char    => TheUnsafe.putChar(_, _, c)
                case obj        => TheUnsafe.putObject(_, _, obj)
            }
            action(instance, fieldOffset)
        }
    }

}
