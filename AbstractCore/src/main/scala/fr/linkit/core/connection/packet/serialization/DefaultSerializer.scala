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

package fr.linkit.core.connection.packet.serialization

import fr.linkit.api.connection.packet.serialization.{Serializer, StringRepresentable}
import fr.linkit.api.local.system.{AppLogger, Version}
import fr.linkit.core.connection.packet.serialization.tree.nodes.StringRepresentableNode
import fr.linkit.core.connection.packet.serialization.tree.{NodeFactory, NodeFinder, NodeHolder}

import java.nio.file.Path

object DefaultSerializer extends Serializer with NodeHolder {

    private val nodeFinder = new NodeFinder

    override val signature: Array[Byte] = Array(4)

    override def serialize(serializable: Serializable, withSignature: Boolean): Array[Byte] = {
        AppLogger.debug(s"Serializing $serializable.")
        val node  = nodeFinder.getSerialNodeForRef(serializable)
        val bytes = node.serialize(serializable, true)

        if (withSignature) signature ++ bytes else bytes
    }

    override def isSameSignature(bytes: Array[Byte]): Boolean = bytes.startsWith(signature)

    override def deserialize(bytes: Array[Byte]): Any = {
        val node = nodeFinder.getDeserialNodeFor(bytes.drop(1))
        node.deserialize()
    }

    override def deserializeAll(bytes: Array[Byte]): Array[Any] = {
        val node = nodeFinder.getDeserialNodeFor[Array[Any]](bytes.drop(1))
        node.deserialize()
    }

    override def attachFactory(nodeFactory: NodeFactory[_]): Unit = nodeFinder.attachFactory(nodeFactory)

    nodeFinder.attachFactory(StringRepresentableNode(Version))
    nodeFinder.attachFactory(StringRepresentableNode(PathRepresentable))

    private object PathRepresentable extends StringRepresentable[Path] {

        override def getRepresentation(t: Path): String = t.toString

        override def fromRepresentation(str: String): Path = Path.of(str)
    }

}
