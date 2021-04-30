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

import fr.linkit.core.connection.packet.serialization.tree.SerialContext.ClassProfile
import fr.linkit.core.connection.packet.serialization.tree._
import fr.linkit.core.local.utils.ScalaUtils.toPresentableString
import fr.linkit.core.local.utils.{NumberSerializer, ScalaUtils}
import sun.misc.Unsafe

import scala.util.Try

object ObjectNode {

    val Constraints   : Array[Class[_] => Boolean] = Array(_.isPrimitive, _.isArray, _.isEnum, _ == classOf[String])
    val NullObjectFlag: Byte                       = -76

    def apply: NodeFactory[Any] = new NodeFactory[Any] {
        override def canHandle(clazz: Class[_]): Boolean = {
            !Constraints.exists(_ (clazz))
        }

        override def canHandle(bytes: ByteSeq): Boolean = {
            bytes.sameFlag(NullObjectFlag) || bytes.isClassDefined
        }

        override def newNode(finder: SerialContext, profile: ClassProfile[Any]): SerialNode[Any] = {
            new ObjectSerialNode(profile, finder)
        }

        override def newNode(finder: SerialContext, bytes: ByteSeq): DeserialNode[Any] = {
            new ObjectDeserialNode(finder.getClassProfile(bytes.getHeaderClass), bytes, finder)
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

    class ObjectSerialNode(profile: ClassProfile[Any], context: SerialContext) extends SerialNode[Any] {

        override def serialize(t: Any, putTypeHint: Boolean): Array[Byte] = {
            //println(s"Serializing Object ${t}")

            val desc = profile.desc
            //println(s"Object desc = ${desc}")
            profile.applyAllSerialProcedures(t)

            if (t == null)
                return Array(NullObjectFlag)

            //println(s"t.getClass = ${t.getClass}")
            val children = context.listNodes[Any](profile, t)
            //println(s"children = ${children}")

            val classType = desc.classSignature
            //println(s"t.getClass.getName.hashCode = ${t.getClass.getName.hashCode}")
            //println(s"classType = ${toPresentableString(classType)}")
            //println(s"NumberSerializer.deserializeInt(classType) = ${NumberSerializer.deserializeInt(classType, 0)}")
            val sign = LengthSign.of(t, desc, children).toBytes
            //println(s"sign = ${toPresentableString(sign)}")
            val bytes = classType ++ sign
            //println(s"Result of Object ${t} = ${toPresentableString(bytes)}")
            bytes
        }
    }

    class ObjectDeserialNode(profile: ClassProfile[Any], bytes: ByteSeq, context: SerialContext) extends DeserialNode[Any] {

        override def deserialize(): Any = {
            if (bytes(0) == NullObjectFlag)
                return null

            //println(s"Deserializing object from bytes ${toPresentableString(bytes)}")
            val objectType = bytes.getHeaderClass

            //println(s"objectType = ${objectType}")
            val desc = profile.desc
            //println(s"Object desc = ${desc}")

            val sign     = LengthSign.from(desc.signItemCount, bytes, bytes.length, 4)
            val instance = TheUnsafe.allocateInstance(desc.clazz)

            val fieldValues = for (childBytes <- sign.childrenBytes) yield {
                //println(s"Field bytes = ${toPresentableString(childBytes)}")
                //println(s"childBytes = ${childBytes.mkString("Array(", ", ", ")")}")
                val node = context.getDeserialNodeFor[Any](childBytes)
                //println(s"node = ${node}")
                val result = node.deserialize()
                //println(s"result = ${result}")
                result
            }

            desc.foreachDeserializableFields { (i, field) =>
                //println(s"For object field number (of object $objectType) $i: ")
                //println(s"Deserialized value : ${fieldValues(i)}")
                ScalaUtils.setValue(instance, field, fieldValues(i))
            }

            //println(s"(objectType) = ${objectType}")
            //Try(//println(s"Instance = $instance"))
            profile.applyAllDeserialProcedures(instance)
            instance.asInstanceOf[Serializable]
        }

    }

}
