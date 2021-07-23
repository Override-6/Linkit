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

import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.DeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.{DeserialisationInputStream, DeserialisationProgression}
import fr.linkit.engine.connection.packet.persistence.v3.ArraySign
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.RawObjectNode
import fr.linkit.engine.local.utils.NumberSerializer

class DefaultDeserialisationProgression(in: DeserialisationInputStream) extends DeserialisationProgression {

    private var poolObject     : Array[Any]              = _
    private var poolObjectNodes: Array[DeserializerNode] = _ //maybe null except during pool initialisation.

    def initPool(): Unit = {
        val length = NumberSerializer.deserializeFlaggedNumber[Int](in)
        val count  = NumberSerializer.deserializeFlaggedNumber[Int](in)
        poolObject = new Array[Any](count)
        in.limit(length + in.get(0) + 1)
        ArraySign.in(count, this, in).getNode(nodes => {
            poolObjectNodes = nodes
            for (i <- nodes.indices if poolObject(i) == null) {
                poolObject(i) = nodes(i).getObject(in)
            }
        }).getObject(in) //.getObject required in order to end the ArraySign action but the returned object is ignored as we do not define it in the lambda above.
        poolObjectNodes = null
        in.limit(in.capacity())
    }

    override def getHeaderObjectNode(place: Int): DeserializerNode = {
        var obj = poolObject(place)
        if (obj == null) {
            if (poolObjectNodes == null)
                throw new UnsupportedOperationException("Attempted to deserialize a Header object in the pool when being outside of the pool initialisation.")
            obj = poolObjectNodes(place).getObject(in)
            poolObject(place) = obj
        }
        RawObjectNode(obj)
    }

}