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

package fr.linkit.api.connection.packet.persistence.v3.serialisation

import fr.linkit.api.connection.packet.PacketCoordinates
import fr.linkit.api.connection.packet.persistence.v3.PacketPersistenceContext
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.SerializerNode

trait SerialisationProgression {

    val context: PacketPersistenceContext
    val pool   : SerializationObjectPool

    val coordinates: PacketCoordinates

    def getSerializationNode(obj: Any): SerializerNode

    def putObject(key: AnyRef, value: AnyRef): Unit

    def getObject[A <: AnyRef](key: AnyRef): Option[A]

    def getOrElseUpdate[A <: AnyRef](key: AnyRef, orElse: => A): A

}
