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

package fr.linkit.engine.connection.packet.persistence.v3.serialisation.helper

import fr.linkit.api.connection.packet.persistence.tree.SerialNode
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialOutputStream
import fr.linkit.engine.connection.packet.persistence.tree.nodes.ArrayNode.ArrayFlag
import fr.linkit.engine.connection.packet.persistence.tree.nodes.ObjectNode.NullObjectFlag
import fr.linkit.engine.local.utils.NumberSerializer
import fr.linkit.engine.local.utils.NumberSerializer.serializeNumber

object ArraySerialisation {

    def serialize(array: Array[Any]): Array[Byte] = {
        val (compType, depth) = getAbsoluteCompType(array)
        val arrayTypeBytes    = NumberSerializer.serializeInt(compType.getName.hashCode)
        val head              = arrayTypeBytes :+ depth :+ ArrayFlag
        if (array.isEmpty) {
            return head ++ ArrayFlag /\ EmptyFlag
        }
        val lengths    = new Array[Int](array.length - 1) //Discard the last field, we already know its length by deducting it from previous lengths
        val byteArrays = new Array[Array[Byte]](array.length)

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
            //println(s"Serializing array item $item ($i)")
            val bytes = lastNode.serialize(cast(item)) //if type have changed, we need to specify the new type.
            //println(s"array item $item into bytes is now ${bytes.mkString("Array(", ", ", ")")}")

            byteArrays(i) = bytes
            if (i != lengths.length)
                lengths(i) = bytes.length

            lastClass = itemClass
        }

        //println(s"lengths = ${lengths.mkString("Array(", ", ", ")")}")
        val sign       = lengths.flatMap(i => serializeNumber(i, true))
        //println(s"array sign = ${toPresentableString(sign)}")
        val signLength = serializeNumber(lengths.length, true)
        //println(s"arrayTypeCode = ${arrayTypeCode}")
        //println(s"array.getClass.getComponentType = ${array.getClass.getComponentType}")
        val result     = head ++ signLength ++ sign ++ byteArrays.flatten
        //println(s"result = ${toPresentableString(result)}")
        result
    }

}
