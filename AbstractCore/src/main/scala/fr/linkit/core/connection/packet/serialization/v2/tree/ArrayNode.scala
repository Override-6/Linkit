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

import fr.linkit.core.connection.packet.serialization.v2.tree.ClassTree.MegaByte
import fr.linkit.core.local.utils.NumberSerializer

object ArrayNode extends NodeFactory[Array[_]] {

    val ArrayFlag: Byte = -54

    override def canHandle(clazz: Class[_]): Boolean = {
        println(s"$clazz.isArray = ${clazz.isArray}")
        clazz.isArray
    }

    override def canHandle(bytes: Array[Byte]): Boolean = {
        bytes.nonEmpty && bytes(0) == ArrayFlag
    }

    override def newNode(tree: ClassTree, desc: SerializableClassDescription, parent: SerialNode[_]): SerialNode[Array[_]] = {
        new ArraySerialNode(parent, tree)
    }

    override def newNode(tree: ClassTree, bytes: Array[Byte], parent: DeserialNode[_]): DeserialNode[Array[_]] = {
        new ArrayDeserialNode(parent, bytes, tree)
    }

    class ArraySerialNode(val parent: SerialNode[_], tree: ClassTree) extends SerialNode[Array[_]] {

        override def serialize(array: Array[_], putTypeHint: Boolean): Array[Byte] = {
            val lengths    = new Array[Int](array.length - 1) //Discard the last field, we already know his length by deducting it from previous lengths
            val byteArrays = new Array[Array[Byte]](array.length)

            var lastClass: Class[_]      = null
            var lastNode : SerialNode[_] = null

            var i = 0
            for (item <- array) {
                val itemClass  = item.getClass
                val typeChange = itemClass != lastClass
                if (typeChange)
                    lastNode = tree.getNodeForRef(itemClass)
                val bytes = lastNode.serialize(cast(item), typeChange) //if type have changed, we need to specify the new type.

                byteArrays(i) = bytes
                if (i != lengths.length)
                    lengths(i) = bytes.length

                i += 1

                lastClass = itemClass
            }
            val sign = lengths.flatMap(i => NumberSerializer.serializeNumber(i, true))
            val signLength = NumberSerializer.serializeNumber(lengths.length - 1, true)
            ArrayFlag /\ signLength ++ sign ++ byteArrays.flatten
        }

        private def cast[T](any: Any): T = {
            any.asInstanceOf[T]
        }
    }

    class ArrayDeserialNode(val parent: DeserialNode[_], bytes: Array[Byte], tree: ClassTree) extends DeserialNode[Array[_]] {

        override def deserialize(): Array[_] = {
            val (signItemCount, sizeByteCount) = NumberSerializer.deserializeFlaggedNumber(bytes, 1)
            val sign                   = LengthSign.from(signItemCount, bytes, bytes.length, sizeByteCount + 1)
            val result                 = new Array[Any](sign.childrenBytes.length)

            var i = 0
            for (childBytes <- sign.childrenBytes) {
                val node = tree.getNodeFor(childBytes, this)
                result(i) = node.deserialize()
                i += 1
            }
            result
        }
    }

}