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
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.RawObjectNode
import fr.linkit.engine.local.utils.NumberSerializer

class DefaultDeserialisationProgression(in: DeserialisationInputStream) extends DeserialisationProgression {

    private val poolObject = {
        val length = NumberSerializer.deserializeFlaggedNumber[Int](in)
        in.limit(length + in.get(0) + 1)
        val array = in.readArray()
        in.limit(in.capacity())
        array
    }

    override def getHeaderObjectNode(place: Int): DeserializerNode = {
        var obj = poolObject(place)
        if (obj == null) {
            obj = in.readObject()
            poolObject(place) = obj
        }
        RawObjectNode(obj)
    }

}