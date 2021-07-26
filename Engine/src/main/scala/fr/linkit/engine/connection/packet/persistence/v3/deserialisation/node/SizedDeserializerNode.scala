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
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.DeserializerNode

class SizedDeserializerNode(pos: Int, buffLimit: Int, val node: DeserializerNode) extends DeserializerNode {

    println(s"SizedDeserializerNode created ! (pos: $pos, buffLimit: $buffLimit)")

    override def deserialize(in: DeserializationInputStream): Any = {
        println(s"Deserializing at pos $pos, and limit $buffLimit.")
        in.limit(buffLimit)
        in.position(pos)
        node.deserialize(in)
    }

}

object SizedDeserializerNode {

    def unapply(arg: SizedDeserializerNode): Option[DeserializerNode] = {
        Some(arg.node)
    }
}
