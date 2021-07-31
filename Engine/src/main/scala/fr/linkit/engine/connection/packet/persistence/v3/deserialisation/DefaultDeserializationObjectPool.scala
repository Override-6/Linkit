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

package fr.linkit.engine.connection.packet.persistence.v3.deserialisation

import fr.linkit.api.connection.packet.persistence.v3.deserialisation.{DeserializationInputStream, DeserializationObjectPool, DeserializationProgression}
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.{DeserializerNode, ObjectDeserializerNode}
import fr.linkit.engine.connection.packet.persistence.MalFormedPacketException
import fr.linkit.engine.connection.packet.persistence.v3.ArraySign
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.{RawObjectNode, SizedDeserializerNode}
import fr.linkit.engine.local.utils.NumberSerializer

class DefaultDeserializationObjectPool(in: DeserializationInputStream) extends DeserializationObjectPool {
    private var poolObject            : Array[Any]                    = _
    private var nonAvailableReferences: Array[ObjectDeserializerNode] = _

    private def fillPool(nodes: Seq[DeserializerNode], postInit: Boolean): Unit = {
        def carefulDeserial(i: Int, refSetter: (Any => Unit) => Unit, node: DeserializerNode): Unit = {
            refSetter { ref =>
                poolObject(i) = ref
                nonAvailableReferences(i) = null //The reference became available
            }
            if (postInit)
                node.deserialize(in)
        }

        for (i <- nodes.indices) {
            nodes(i) match {
                case node: ObjectDeserializerNode =>
                    carefulDeserial(i, action => node.addOnReferenceAvailable(action), node)
                    nonAvailableReferences(i) = node
                case e: SizedDeserializerNode     =>
                    e.node match {
                        case node: ObjectDeserializerNode =>
                            carefulDeserial(i, action => node.addOnReferenceAvailable(action), e)
                            nonAvailableReferences(i) = node
                        case _                            =>
                            if (!postInit)
                                poolObject(i) = e.deserialize(in)

                    }
                case node                         =>
                    if (!postInit)
                        poolObject(i) = node.deserialize(in)
            }
            if (nodes(i) == null && postInit)
                throw new MalFormedPacketException("Packet pool contains null elements")
        }
    }

    override def getHeaderValueNode(place: Int): DeserializerNode = {
        val obj = poolObject(place)
        if (obj == null) {
            if (nonAvailableReferences == null || nonAvailableReferences(place) == null)
                throw new NullPointerException("Unexpected null item in poolObject")
            else nonAvailableReferences(place)
        }
        RawObjectNode(obj)
    }

    override def initPool(progess: DeserializationProgression): Unit = {
        if (poolObject != null)
            throw new IllegalStateException("This object pool is already initialised !")
        val buff   = in.buff
        val length = NumberSerializer.deserializeFlaggedNumber[Int](buff)
        val count  = NumberSerializer.deserializeFlaggedNumber[Int](buff)

        poolObject = new Array(count)
        nonAvailableReferences = new Array(count)

        buff.limit(length + buff.position())
        var maxPos = 0

        ArraySign.in(count, progess, in).deserializeRef(poolObject)(nodes => {
            fillPool(nodes, false)
            maxPos = buff.position()
            fillPool(nodes, true)
            maxPos = Math.max(maxPos, buff.position())
        }).deserialize(in)

        buff.limit(buff.capacity())
        buff.position(maxPos)

        nonAvailableReferences = null
    }
}
