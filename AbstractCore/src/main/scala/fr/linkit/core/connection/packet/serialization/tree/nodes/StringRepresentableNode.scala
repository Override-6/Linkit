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
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.packet.serialization.tree._

import scala.reflect.{ClassTag, classTag}

object StringRepresentableNode {

    private val SRFlag: Array[Byte] = Array(-111)

    def apply[T: ClassTag](repr: StringRepresentable[T]): NodeFactory[T] = new NodeFactory[T] {
        private val clazz = classTag[T].runtimeClass

        override def canHandle(clazz: Class[_]): Boolean = this.clazz.isAssignableFrom(clazz)

        override def canHandle(bytes: Array[Byte]): Boolean = {
            bytes.nonEmpty && bytes(0) == SRFlag(0)
        }

        override def newNode(finder: NodeFinder, desc: SerializableClassDescription, parent: SerialNode[_]): SerialNode[T] = {
            new StringRepresentableSerialNode[T](parent, finder, repr)
        }

        override def newNode(finder: NodeFinder, bytes: Array[Byte], parent: DeserialNode[_]): DeserialNode[T] = {
            new StringRepresentableDeserialNode[T](parent, finder, bytes, repr)
        }
    }

    class StringRepresentableSerialNode[T](override val parent: SerialNode[_], finder: NodeFinder, repr: StringRepresentable[T]) extends SerialNode[T] {

        override def serialize(t: T, putTypeHint: Boolean): Array[Byte] = {
            val node = finder.getSerialNodeForType[String](classOf[String], parent)
            println(s"node = ${node}")
            //Thread.dumpStack()
            SRFlag ++ node.serialize(repr.getRepresentation(t), putTypeHint)
        }
    }

    class StringRepresentableDeserialNode[T](override val parent: DeserialNode[_],
                                             finder: NodeFinder, bytes: Array[Byte],
                                             repr: StringRepresentable[T]) extends DeserialNode[T] {

        override def deserialize(): T = {
            val str = finder.getDeserialNodeFor[String](bytes.drop(1)).deserialize()
            repr.fromRepresentation(str)
        }
    }

}
