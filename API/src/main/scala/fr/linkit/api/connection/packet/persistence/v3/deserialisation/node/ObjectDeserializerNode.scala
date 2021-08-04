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

package fr.linkit.api.connection.packet.persistence.v3.deserialisation.node

import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserializationInputStream

import scala.collection.mutable

trait ObjectDeserializerNode extends DeserializerNode {

    protected val listeners: mutable.ListBuffer[Any => Unit] = mutable.ListBuffer.empty

    protected var ref: Any

    def addOnReferenceAvailable(action: Any => Unit): Unit = {
        if (ref == null)
            listeners += action
        else action(ref)
    }

    def isDeserializing: Boolean

    override def deserialize(in: DeserializationInputStream): Any

}
