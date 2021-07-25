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

package fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node

import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserializationInputStream
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.ObjectDeserializerNode
import fr.linkit.engine.local.utils.JavaUtils

class SimpleObjectDeserializerNode(override protected var ref: AnyRef, deserializeAction: DeserializationInputStream => Any) extends ObjectDeserializerNode {

    override def deserialize(in: DeserializationInputStream): Any = {
        val returned = deserializeAction(in)
        if (!JavaUtils.sameInstance(returned, ref))
            throw new RuntimeException("The returned value reference is not equals to the expected reference stored in SimpleObjectDeserializerNode.ref.")
        returned //or ref, this is the same object.
    }

}

object SimpleObjectDeserializerNode {

    def apply(ref: AnyRef)(deserialize: DeserializationInputStream => Any): SimpleObjectDeserializerNode = new SimpleObjectDeserializerNode(ref, deserialize)
}
