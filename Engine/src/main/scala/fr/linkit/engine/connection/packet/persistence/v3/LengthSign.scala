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

package fr.linkit.engine.connection.packet.persistence.v3

import fr.linkit.api.connection.packet.persistence.v3.PersistenceContext
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.DeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.{DeserialisationInputStream, DeserialisationProgression}
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.SerializerNode
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.SizedDeserializerNode
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.DefaultSerialisationOutputStream
import fr.linkit.engine.local.utils.NumberSerializer

import java.nio.ByteBuffer
import scala.collection.mutable.ListBuffer

sealed trait LengthSign

object LengthSign {

    case class LengthSignOut(context: PersistenceContext, progress: SerialisationProgression, childrenNodes: Array[SerializerNode]) extends LengthSign {

        def getNode: SerializerNode = {
            out => {
                val lengths = ListBuffer.empty[Int]
                val buff    = out.buff
                val fakeOut = new DefaultSerialisationOutputStream(ByteBuffer.allocate(buff.limit() - buff.position()), progress, context)
                childrenNodes.foreach(node => {
                    val pos0 = fakeOut.position()
                    node.writeBytes(fakeOut)
                    val pos1 = fakeOut.position()
                    lengths += pos1 - pos0
                })
                val lengthsBytes = lengths.drop(1).flatMap(l => NumberSerializer.serializeNumber(l, true)).toArray
                out.put(lengthsBytes)
                        .put(fakeOut.array(), 0, fakeOut.position())
            }
        }
    }

    case class LengthSignIn(context: PersistenceContext, progress: DeserialisationProgression, lengths: Array[Int]) extends LengthSign {

        def getNode(group: Array[DeserializerNode] => DeserializerNode): DeserializerNode = {
            in => {
                val buff  = in.buff
                val nodes = new Array[DeserializerNode](lengths.length)
                for (i <- lengths.indices) {
                    val limit = buff.position() + lengths(i)
                    val pos   = buff.position() + (if (i == 0) 0 else lengths(i - 1))
                    buff.position(pos)
                    nodes(i) = new SizedDeserializerNode(limit, context.getDeserializationNode(in, progress))
                }
                group(nodes)
            }
        }
    }

    def out(values: Seq[Any], progress: SerialisationProgression, context: PersistenceContext): LengthSignOut = {
        val signItemCount = values.length
        if (signItemCount == 0)
            return LengthSignOut(context, progress, Array())

        val childrenNodes = new Array[SerializerNode](signItemCount)

        for (i <- values.indices) {
            val fieldValue = values(i)
            val node       = context.getSerializationNode(fieldValue, progress)
            childrenNodes(i) = node
        }
        LengthSignOut(context, progress, childrenNodes)
    }

    def in(signItemCount: Int, context: PersistenceContext, progress: DeserialisationProgression, in: DeserialisationInputStream): LengthSignIn = {

        val totalItemCount = signItemCount + 1
        if (signItemCount == -1)
            return LengthSignIn(context, progress, Array())

        val lengths = new Array[Int](totalItemCount)
        for (i <- 0 to signItemCount) {
            if (i != signItemCount)
                lengths(i) = NumberSerializer.deserializeFlaggedNumber[Int](in)
            else lengths(i) = in.limit() - (if (i == 0) in.position() else lengths.sum)

        }

        LengthSignIn(context, progress, lengths)
    }

}
