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

import fr.linkit.core.local.utils.NumberSerializer

case class LengthSign(lengths: Array[Int], childrenBytes: Array[Array[Byte]]) {

    def toBytes: Array[Byte] = {
        lengths.flatMap(l => NumberSerializer.serializeNumber(l, true)) ++ childrenBytes.flatten
    }

}

object LengthSign {

    def of(root: Any, desc: SerializableClassDescription, children: Iterable[SerialNode[_]]): LengthSign = {
        val lengths    = new Array[Int](children.size - 1) //Discard the last field, we already know his length by deducting it from previous lengths
        val byteArrays = new Array[Array[Byte]](children.size)

        val fieldValues = desc.serializableFields
                .map(_.get(root))

        var i = 0
        for (child <- children) {
            val fieldValue = fieldValues(i)
            println(s"fieldValue = ${fieldValue}")
            println(s"child = ${child}")
            val bytes = child.serialize(cast(fieldValue), true)

            byteArrays(i) = bytes
            if (i < lengths.length)
                lengths(i) = bytes.length
            i += 1
        }
        LengthSign(lengths, byteArrays)
    }

    def from(signItemCount: Int, bytes: Array[Byte], totalObjectLength: Int, start: Int): LengthSign = {
        val lengths            = new Array[Int](signItemCount) //Discard the last field, we already know his length by deducting it from totalObjectLength
        val childrenByteArrays = new Array[Array[Byte]](signItemCount + 1)

        var currentIndex = start
        for (i <- 0 until signItemCount) {
            val (length, lengthByteCount) = NumberSerializer.deserializeFlaggedNumber(bytes, currentIndex)
            lengths(i) = length
            currentIndex += lengthByteCount
        }

        for (i <- 0 until (signItemCount + 1)) {
            val childrenSize = if (i == lengths.length) currentIndex - totalObjectLength else lengths(i)

            val childrenBytes = bytes.slice(currentIndex, currentIndex + childrenSize)
            childrenByteArrays(i) = childrenBytes
            currentIndex += childrenSize
        }
        LengthSign(lengths, childrenByteArrays)
    }

    private def cast[T](any: Any): T = {
        any.asInstanceOf[T]
    }

}
