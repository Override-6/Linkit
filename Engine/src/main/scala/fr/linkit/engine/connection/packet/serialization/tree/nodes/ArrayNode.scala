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

package fr.linkit.engine.connection.packet.serialization.tree.nodes

import fr.linkit.engine.connection.packet.serialization.tree.SerialContext.{ClassProfile, MegaByte}
import fr.linkit.engine.connection.packet.serialization.tree._
import fr.linkit.engine.connection.packet.serialization.tree.nodes.ObjectNode.NullObjectFlag
import fr.linkit.engine.local.utils.NumberSerializer
import fr.linkit.engine.local.utils.ScalaUtils.toPresentableString

import scala.util.Try

object ArrayNode extends NodeFactory[Array[_]] {

    val ArrayFlag: Byte = -54
    val EmptyFlag: Byte = -53

    override def canHandle(clazz: Class[_]): Boolean = {
        clazz.isArray
    }

    override def canHandle(bytes: ByteSeq): Boolean = {
        bytes.sameFlag(ArrayFlag)
    }

    override def newNode(finder: SerialContext, profile: ClassProfile[Array[_]]): SerialNode[Array[_]] = {
        new ArraySerialNode(profile, finder)
    }

    override def newNode(finder: SerialContext, bytes: ByteSeq): DeserialNode[Array[_]] = {
        new ArrayDeserialNode(finder.getProfile[Array[_]], bytes, finder)
    }

    class ArraySerialNode(profile: ClassProfile[Array[_]], tree: SerialContext) extends SerialNode[Array[_]] {

        override def serialize(array: Array[_], putTypeHint: Boolean): Array[Byte] = {
            //println(s"Serializing array ${array.mkString("Array(", ", ", ")")}")
            profile.applyAllSerialProcedures(array)
            if (array.isEmpty) {
                return ArrayFlag /\ EmptyFlag
            }
            val lengths    = new Array[Int](array.length - 1) //Discard the last field, we already know its length by deducting it from previous lengths
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
                    lastNode = tree.getSerialNodeForType(itemClass)
                //println(s"Serializing array item $item ($i)")
                val bytes = lastNode.serialize(cast(item), typeChange) //if type have changed, we need to specify the new type.
                //println(s"array item $item into bytes is now ${bytes.mkString("Array(", ", ", ")")}")

                byteArrays(i) = bytes
                if (i != lengths.length)
                    lengths(i) = bytes.length

                lastClass = itemClass
            }

            //println(s"lengths = ${lengths.mkString("Array(", ", ", ")")}")
            val sign = lengths.flatMap(i => NumberSerializer.serializeNumber(i, true))
            //println(s"array sign = ${toPresentableString(sign)}")
            val signLength = NumberSerializer.serializeNumber(lengths.length, true)
            //println(s"sign length = ${signLength.mkString("Array(", ", ", ")")}")
            val result = ArrayFlag /\ signLength ++ sign ++ byteArrays.flatten
            //println(s"result = ${toPresentableString(result)}")
            result
        }

        private def cast[T](any: Any): T = {
            any.asInstanceOf[T]
        }
    }

    class ArrayDeserialNode(profile: ClassProfile[Array[_]], bytes: Array[Byte], context: SerialContext) extends DeserialNode[Array[_]] {

        override def deserialize(): Array[_] = {
            if (bytes(1) == EmptyFlag)
                return Array.empty

            //println(s"Deserializing array into bytes ${toPresentableString(bytes)}")
            val (signItemCount, sizeByteCount: Byte) = NumberSerializer.deserializeFlaggedNumber[Int](bytes, 1) //starting from 1 because first byte is the array flag.
            //println(s"signItemCount = ${signItemCount}")
            //println(s"sizeByteCount = ${sizeByteCount}")
            val sign   = LengthSign.from(signItemCount, bytes, bytes.length, sizeByteCount + 1)
            val result = new Array[Any](sign.childrenBytes.length)

            var i = 0
            for (childBytes <- sign.childrenBytes) {
                //println(s"ITEM Deserial $i:")
                val node = context.getDeserialNodeFor(childBytes)
                //println(s"node = ${node}")
                result(i) = node.deserialize()
                //Try(//println(s"array item deserialize result = ${result(i)}"))
                i += 1
            }

            profile.applyAllDeserialProcedures(result)
            result

        }
    }

}
