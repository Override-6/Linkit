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

import fr.linkit.core.connection.packet.serialization.tree.{DeserialNode, NodeFactory, NodeFinder, SerialNode, SerializableClassDescription}

object StringNode extends NodeFactory[String] {

    private val StringFlag: Array[Byte] = Array(-101)

    override def canHandle(clazz: Class[_]): Boolean = clazz == classOf[String]

    override def canHandle(bytes: Array[Byte]): Boolean = bytes.nonEmpty && bytes(0) == StringFlag(0)

    override def newNode(finder: NodeFinder, desc: SerializableClassDescription, parent: SerialNode[_]): SerialNode[String] = new StringSerialNode(parent)

    override def newNode(finder: NodeFinder, bytes: Array[Byte], parent: DeserialNode[_]): DeserialNode[String] = new StringDeserialNode(parent, bytes)

    class StringSerialNode(override val parent: SerialNode[_]) extends SerialNode[String] {

        override def serialize(t: String, putTypeHint: Boolean): Array[Byte] = {
            StringFlag ++ t.getBytes()
        }
    }

    class StringDeserialNode(override val parent: DeserialNode[_], bytes: Array[Byte]) extends DeserialNode[String] {

        override def deserialize(): String = new String(bytes.drop(1))
    }

}
