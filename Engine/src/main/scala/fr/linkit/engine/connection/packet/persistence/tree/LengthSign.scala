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

package fr.linkit.engine.connection.packet.persistence.tree

import fr.linkit.api.connection.packet.persistence.tree.SerializableClassDescription
import fr.linkit.api.connection.packet.persistence.v3.PersistenceContext
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.SerializerNode
import fr.linkit.api.connection.packet.persistence.v3.serialisation.{SerialOutputStream, SerialisationProgression}
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.DefaultSerialOutputStream
import fr.linkit.engine.local.utils.NumberSerializer

import java.nio.ByteBuffer
import scala.collection.mutable.ListBuffer

case class LengthSign(context: PersistenceContext, childrenNodes: Array[SerializerNode]) {

    def getNode: SerializerNode = out => {
        val lengths = ListBuffer.empty[Int]
        val buff    = out.buff
        val fakeOut = new DefaultSerialOutputStream(ByteBuffer.allocate(buff.position() - buff.limit()), context)
        childrenNodes.foreach(node => {
            val pos0 = fakeOut.position()
            node.writeBytes(fakeOut)
            val pos1 = fakeOut.position()
            lengths += pos1 - pos0
        })
        val lengthsBytes = lengths.flatMap(l => NumberSerializer.serializeNumber(l, true)).toArray
        out.put(lengthsBytes)
                .put(fakeOut)
    }

}

object LengthSign {

    def of(root: Any, desc: SerializableClassDescription, context: PersistenceContext): LengthSign = {
        val signItemCount = desc.signItemCount
        if (signItemCount == -1)
            return LengthSign(context, Array())

        val childrenNodes = new Array[SerializerNode](signItemCount)

        val fieldValues = desc.serializableFields
                .map(_.first.get(root))

        for (i <- desc.serializableFields.indices) {
            val fieldValue = fieldValues(i)
            val node       = context.getNode(fieldValue)
            childrenNodes(i) = node
        }
        LengthSign(context, childrenNodes)
    }

    def from(signItemCount: Int, bytes: Array[Byte], totalObjectLength: Int, start: Int): LengthSign = {
        //println()
        //println(s"--- Reading sign from bytes ${ScalaUtils.toPresentableString(bytes.drop(start))}")
        //println(s"signItemCount = ${signItemCount}")
        //println(s"totalObjectLength = ${totalObjectLength}")
        //println(s"start = ${start}")

        if (signItemCount == -1)
            return LengthSign(Array(), Array())

        val lengths            = new Array[Int](signItemCount) //Discard the last field, we already know its length by deducting it from totalObjectLength
        val childrenByteArrays = new Array[Array[Byte]](signItemCount + 1)

        var currentIndex = start
        //println("-- Reading lengths ")
        for (i <- 0 until signItemCount) {
            //println(s"FOR LENGTH ${i}: ")
            val (length, lengthByteCount: Byte) = NumberSerializer.deserializeFlaggedNumber[Int](bytes, currentIndex)
            lengths(i) = length
            //println(s"length = ${length}")
            //println(s"lengthByteCount = ${lengthByteCount}")
            //println(s"lengths = ${lengths.mkString("Array(", ", ", ")")}")
            currentIndex += lengthByteCount
            //println(s"currentIndex is now = ${currentIndex}")
        }

        //println("-- Reading children bytes")
        for (i <- 0 to signItemCount) {
            //println(s"- FOR CHILD ${i}: ")
            val childrenSize  = if (i == lengths.length) totalObjectLength - currentIndex else lengths(i)
            //println(s"childrenSize = ${childrenSize}")
            //println(s"currentIndex = ${currentIndex}")
            val childrenBytes = bytes.slice(currentIndex, currentIndex + childrenSize)
            //println(s"childrenBytes = ${childrenBytes.mkString("Array(", ", ", ")")}")
            //println(s"bytes = ${bytes.mkString("Array(", ", ", ")")}")
            //println(s"childrenBytes = ${ScalaUtils.toPresentableString(childrenBytes)}")
            childrenByteArrays(i) = childrenBytes
            currentIndex += childrenSize
        }
        //println()
        LengthSign(lengths, childrenByteArrays)
    }

    @inline private def cast[T](any: Any): T = {
        any.asInstanceOf[T]
    }

}
