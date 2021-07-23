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
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.SerializerNode
import fr.linkit.api.connection.packet.persistence.v3.serialisation.{SerialisationOutputStream, SerialisationProgression}
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.SizedDeserializerNode
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.DefaultSerialisationOutputStream
import fr.linkit.engine.local.utils.NumberSerializer

import java.nio.ByteBuffer
import scala.collection.mutable.ListBuffer

sealed trait ArraySign

object ArraySign {

    case class ArraySignOut(context: PersistenceContext, progress: SerialisationProgression, childrenNodes: Array[SerializerNode]) extends ArraySign {

        def getNode: SerializerNode = {
            out => {
                val lengths = ListBuffer.empty[Int]
                val buff    = out.buff
                val fakeOut = new DefaultSerialisationOutputStream(ByteBuffer.allocate(buff.limit() - buff.position()), progress, context)
                childrenNodes.foreach(node => {
                    val pos0 = fakeOut.position()
                    progress.addSerialisationDepth()
                    node.writeBytes(fakeOut)
                    progress.removeSerialisationDepth()
                    val pos1 = fakeOut.position()
                    lengths += pos1 - pos0
                })
                val lengthsBytes = lengths.dropRight(1).flatMap(l => NumberSerializer.serializeNumber(l, true)).toArray
                out.put(lengthsBytes)
                        .put(fakeOut.array(), 0, fakeOut.position())
            }
        }
    }

    case class ArraySignIn(context: PersistenceContext, progress: DeserialisationProgression, lengths: Array[Int]) extends ArraySign {

        def getNode(group: Array[DeserializerNode] => Any): DeserializerNode = {
            in => {
                val buff      = in.buff
                val nodes     = new Array[DeserializerNode](lengths.length)
                var nodeStart = buff.position()
                for (i <- lengths.indices) {
                    buff.position(nodeStart)
                    val nodeLength = lengths(i)
                    val limit      = nodeStart + nodeLength
                    val node       = context.getDeserializationNode(in, progress)
                    nodes(i) = new SizedDeserializerNode(buff.position(), limit, node)
                    nodeStart += nodeLength
                }
                group(nodes)
            }
        }
    }

    def out(values: Seq[Any], out: SerialisationOutputStream, progress: SerialisationProgression, context: PersistenceContext): ArraySignOut = {
        val signItemCount = values.length
        if (signItemCount == 0)
            return ArraySignOut(context, progress, Array())

        val childrenNodes = new Array[SerializerNode](signItemCount)

        for (i <- values.indices) {
            val fieldValue = values(i)
            progress.addSerialisationDepth()
            val node = context.getSerializationNode(fieldValue, out, progress)
            progress.removeSerialisationDepth()
            childrenNodes(i) = node
        }
        ArraySignOut(context, progress, childrenNodes)
    }

    def in(signItemCount: Int, progress: DeserialisationProgression, in: DeserialisationInputStream): ArraySignIn = {
        val totalItemCount = signItemCount + 1
        if (totalItemCount == 0)
            return ArraySignIn(in.context, progress, Array())

        val lengths = new Array[Int](totalItemCount)
        for (i <- 0 to signItemCount) {
            if (i != signItemCount)
                lengths(i) = NumberSerializer.deserializeFlaggedNumber[Int](in)
            else lengths(i) = in.limit() - in.position() - (if (i == 0) 0 else lengths.sum)
        }

        ArraySignIn(in.context, progress, lengths)
    }

}
