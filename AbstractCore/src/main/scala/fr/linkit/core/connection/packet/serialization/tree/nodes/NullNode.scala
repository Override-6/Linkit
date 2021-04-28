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

import fr.linkit.core.connection.packet.serialization.tree.{ByteSeqInfo, DeserialNode, NodeFactory, NodeFinder, SerialNode, SerializableClassDescription}

object NullNode extends NodeFactory[Null] {

    val NullFlag: Array[Byte] = Array(-74)

    class NullSerial(override val parent: SerialNode[_]) extends SerialNode[Null] {

        override def serialize(t: Null, putTypeHint: Boolean): Array[Byte] = NullFlag
    }

    class NullDeserial(override val parent: DeserialNode[_]) extends DeserialNode[Null] {

        override def deserialize(): Null = null
    }

    override def canHandle(clazz: Class[_]): Boolean = clazz == null

    override def canHandle(bytes: ByteSeqInfo): Boolean = bytes.sameFlag(NullFlag(0))

    override def newNode(finder: NodeFinder, desc: SerializableClassDescription, parent: SerialNode[_]): SerialNode[Null] = {
        new NullSerial(parent)
    }

    override def newNode(finder: NodeFinder, bytes: Array[Byte], parent: DeserialNode[_]): DeserialNode[Null] = {
        new NullDeserial(parent)
    }
}
