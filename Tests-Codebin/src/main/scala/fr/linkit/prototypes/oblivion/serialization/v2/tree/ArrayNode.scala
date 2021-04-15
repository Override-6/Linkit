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

import NodeFinder.MegaByte
import ObjectNode.NullObjectFlag
import fr.linkit.core.local.utils.{NumberSerializer, ScalaUtils}
import fr.linkit.core.local.utils.ScalaUtils.toPresentableString


object ArrayNode extends NodeFactory[Array[_]] {

    val ArrayFlag: Byte = -54
    val EmptyFlag: Byte = -53

    override def canHandle(clazz: Class[_]): Boolean = {
        clazz.isArray
    }

    override def canHandle(bytes: Array[Byte]): Boolean = {
        bytes.nonEmpty && bytes(0) == ArrayFlag
    }

    override def newNode(finder: NodeFinder, desc: SerializableClassDescription, parent: SerialNode[_]): SerialNode[Array[_]] = {
        new ArraySerialNode(parent, finder)
    }

    override def newNode(finder: NodeFinder, bytes: Array[Byte], parent: DeserialNode[_]): DeserialNode[Array[_]] = {
        new ArrayDeserialNode(parent, bytes, finder)
    }

    class ArraySerialNode(val parent: SerialNode[_], tree: NodeFinder) extends SerialNode[Array[_]] {

        override def serialize(array: Array[_], putTypeHint: Boolean): Array[Byte] = {
            println(s"Serializing array ${array.mkString("Array(", ", ", ")")}")
            if (array.isEmpty) {
                return ArrayFlag /\ EmptyFlag
            }
            val lengths    = new Array[Int](array.length - 1) //Discard the last field, we already know his length by deducting it from previous lengths
            val byteArrays = new Array[Array[Byte]](array.length)

            var lastClass: Class[_]      = null
            var lastNode : SerialNode[_] = null

            var i = 0
            for (item <- array) {
                serializeItem(item)
                i += 1
            }

            def serializeItem(item: Any): Unit = {
                if (item == null) {
                    byteArrays(i) = Array(NullObjectFlag)
                    if (i != lengths.length)
                        lengths(i) = 1
                    return
                }
                val itemClass  = item.getClass
                val typeChange = itemClass != lastClass
                if (typeChange)
                    lastNode = tree.getNodeForClass(itemClass)
                println(s"Serializing array item $item ($i)")
                val bytes = lastNode.serialize(cast(item), typeChange) //if type have changed, we need to specify the new type.
                println(s"array item $item into bytes is now ${toPresentableString(bytes)}")

                byteArrays(i) = bytes
                if (i != lengths.length)
                    lengths(i) = bytes.length

                lastClass = itemClass
            }

            println(s"lengths = ${lengths.mkString("Array(", ", ", ")")}")
            val sign = lengths.flatMap(i => NumberSerializer.serializeNumber(i, true))
            println(s"array sign = ${toPresentableString(sign)}")
            val signLength = NumberSerializer.serializeNumber(lengths.length, true)
            println(s"sign length = ${toPresentableString(signLength)}")
            val result = ArrayFlag /\ signLength ++ sign ++ byteArrays.flatten
            println(s"result = ${toPresentableString(result)}")
            result
        }

        private def cast[T](any: Any): T = {
            any.asInstanceOf[T]
        }
    }

    class ArrayDeserialNode(val parent: DeserialNode[_], bytes: Array[Byte], tree: NodeFinder) extends DeserialNode[Array[_]] {

        override def deserialize(): Array[_] = {
            if (bytes(1) == EmptyFlag)
                return Array.empty

            println(s"Deserializing array into bytes ${toPresentableString(bytes)}")
            val (signItemCount, sizeByteCount: Byte) = NumberSerializer.deserializeFlaggedNumber[Int](bytes, 1: Int) //starting from 1 because first byte is the array flag.
            println(s"signItemCount = ${signItemCount}")
            println(s"sizeByteCount = ${sizeByteCount}")
            val sign   = LengthSign.from(signItemCount, bytes, bytes.length, sizeByteCount + 1)
            val result = new Array[Any](sign.childrenBytes.length)

            var i = 0
            for (childBytes <- sign.childrenBytes) {
                println(s"ITEM Deserial $i:")
                val node = tree.getDeserialNodeFor(childBytes, this)
                println(s"node = ${node}")
                result(i) = node.deserialize()
                println(s"array item deserialize result = ${result(i)}")
                i += 1
            }
            result
        }
    }

}