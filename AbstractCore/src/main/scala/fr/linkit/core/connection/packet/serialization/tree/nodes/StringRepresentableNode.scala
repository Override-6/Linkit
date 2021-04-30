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

import fr.linkit.api.connection.packet.serialization.StringRepresentable
import fr.linkit.core.connection.packet.serialization.tree._
import fr.linkit.core.local.utils.{NumberSerializer, ScalaUtils}

import scala.reflect.{ClassTag, classTag}

object StringRepresentableNode {

    private val SRFlag: Array[Byte] = Array(-111)

    def apply[T: ClassTag](repr: StringRepresentable[T]): NodeFactory[T] = new NodeFactory[T] {
        private val clazz = classTag[T].runtimeClass

        override def canHandle(clazz: Class[_]): Boolean = this.clazz.isAssignableFrom(clazz)

        override def canHandle(bytes: ByteSeqInfo): Boolean = {
            println(s"bytes = ${ScalaUtils.toPresentableString(bytes.bytes)}")
            println(s"raw bytes = ${bytes.bytes.mkString("Array(", ", ", ")")}")
            bytes.sameFlag(SRFlag(0)) && bytes.classExists(1, s => {
                println(s"clazz = ${clazz}")
                println(s"s = ${s}")
                clazz.isAssignableFrom(s)
            })
        }

        override def newNode(finder: NodeFinder, desc: SerializableClassDescription, parent: SerialNode[_]): SerialNode[T] = {
            new StringRepresentableSerialNode[T](parent, repr)
        }

        override def newNode(finder: NodeFinder, bytes: Array[Byte], parent: DeserialNode[_]): DeserialNode[T] = {
            new StringRepresentableDeserialNode[T](parent, bytes, repr)
        }
    }

    class StringRepresentableSerialNode[T](override val parent: SerialNode[_], repr: StringRepresentable[T]) extends SerialNode[T] {

        override def serialize(t: T, putTypeHint: Boolean): Array[Byte] = {
            val typeBytes = NumberSerializer.serializeInt(t.getClass.getName.hashCode)
            //Thread.dumpStack()
            SRFlag ++ typeBytes ++ repr.getRepresentation(t).getBytes
        }
    }

    class StringRepresentableDeserialNode[T](override val parent: DeserialNode[_], bytes: Array[Byte],
                                             repr: StringRepresentable[T]) extends DeserialNode[T] {

        override def deserialize(): T = {
            repr.fromRepresentation(new String(bytes.drop(5)))
        }
    }

}
