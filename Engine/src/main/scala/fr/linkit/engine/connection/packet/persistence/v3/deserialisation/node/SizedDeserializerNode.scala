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

import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserialisationInputStream
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.DeserializerNode

class SizedDeserializerNode(pos: Int, buffLimit: Int, node: DeserializerNode) extends DeserializerNode {

    override def getObject(in: DeserialisationInputStream): Any = {
        in.position(pos)
        in.limit(buffLimit)
        node.getObject(in)
    }
}
