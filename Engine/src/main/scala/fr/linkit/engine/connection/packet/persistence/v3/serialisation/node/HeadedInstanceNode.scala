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

package fr.linkit.engine.connection.packet.persistence.v3.serialisation.node

import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationOutputStream
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.SerializerNode
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.SerializerNodeFlags.HeadedObjectFlag
import fr.linkit.engine.local.utils.NumberSerializer

class HeadedInstanceNode(place: Int) extends SerializerNode {

    override def writeBytes(out: SerialisationOutputStream): Unit = {
        out.put(HeadedObjectFlag)
                .put(NumberSerializer.serializeNumber(place, true))
    }

}
