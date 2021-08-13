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

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.packet.persistence.v3.PacketPersistenceContext
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserializationInputStream

abstract class SynchronizedObjectDeserializerNode(context: PacketPersistenceContext) extends SimpleObjectDeserializerNode(context) {

    def deserializeWrapper(ref: AnyRef, in: DeserializationInputStream): SynchronizedObject[_]

    def retrieveAndSetWrapperRef(in: DeserializationInputStream): Unit

    override def deserializeAction(in: DeserializationInputStream): Any = {
        retrieveAndSetWrapperRef(in)
        ref
    }

}
