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

import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.{DeserializerNode, ObjectDeserializerNode}
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.{DeserializationInputStream, DeserializationProgression}
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.SerializerNode
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.{SimpleObjectDeserializerNode, SizedDeserializerNode}
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.DefaultSerialisationOutputStream
import fr.linkit.engine.local.utils.NumberSerializer

import java.nio.ByteBuffer
import scala.collection.mutable.ListBuffer

sealed trait ArraySign

object ArraySign {

    case class ArraySignOut(progress: SerialisationProgression, childrenNodes: Array[SerializerNode]) extends ArraySign {

        def getNode: SerializerNode = {
            out => {
                val pool = progress.pool
                val lengths = ListBuffer.empty[Int]
                val buff    = out.buff
                val fakeOut = new DefaultSerialisationOutputStream(ByteBuffer.allocate(buff.limit() - buff.position()), pool, progress.context)
                childrenNodes.foreach(node => {
                    val pos0 = fakeOut.position()
                    pool.addSerialisationDepth()
                    node.writeBytes(fakeOut)
                    pool.removeSerialisationDepth()
                    val pos1 = fakeOut.position()
                    lengths += pos1 - pos0
                })
                val lengthsBytes = lengths.dropRight(1).flatMap(l => NumberSerializer.serializeNumber(l, true)).toArray
                out.put(lengthsBytes)
                        .put(fakeOut.array(), 0, fakeOut.position())
            }
        }
    }

    case class ArraySignIn(progress: DeserializationProgression, lengths: Array[Int]) extends ArraySign {

        def deserializeRef(ref: AnyRef)(group: Array[DeserializerNode] => Unit): ObjectDeserializerNode = {
            SimpleObjectDeserializerNode(ref) {
                in => {
                    val buff      = in.buff
                    val nodes     = new Array[DeserializerNode](lengths.length)
                    var nodeStart = buff.position()
                    for (i <- lengths.indices) {
                        buff.position(nodeStart)
                        val nodeLength = lengths(i)
                        val limit      = nodeStart + nodeLength
                        val node       = progress.getNextDeserializationNode
                        nodes(i) = new SizedDeserializerNode(buff.position(), limit, node)
                        nodeStart += nodeLength
                    }
                    group(nodes)
                    ref
                }
            }
        }
    }

    def out(values: Seq[_], progress: SerialisationProgression): ArraySignOut = {
        val signItemCount = values.length
        if (signItemCount == 0)
            return ArraySignOut(progress, Array())

        val childrenNodes = new Array[SerializerNode](signItemCount)
        val pool = progress.pool

        for (i <- values.indices) {
            val fieldValue = values(i)
            pool.addSerialisationDepth()
            val node = progress.getSerializationNode(fieldValue)
            pool.removeSerialisationDepth()
            childrenNodes(i) = node
        }
        ArraySignOut(progress, childrenNodes)
    }

    def in(signItemCount: Int, progression: DeserializationProgression, in: DeserializationInputStream): ArraySignIn = {
        val totalItemCount = signItemCount + 1
        if (totalItemCount == 0)
            return ArraySignIn(progression, Array())

        val lengths = new Array[Int](totalItemCount)
        for (i <- 0 to signItemCount) {
            if (i != signItemCount)
                lengths(i) = NumberSerializer.deserializeFlaggedNumber[Int](in)
            else lengths(i) = in.limit() - in.position() - (if (i == 0) 0 else lengths.sum)
        }

        ArraySignIn(progression, lengths)
    }

}
