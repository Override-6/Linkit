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

package fr.linkit.api.connection.packet.persistence.v3

import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.DeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.{DeserialisationInputStream, DeserialisationProgression}
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.DelegatingSerializerNode

trait PersistenceContext {

    def getSerializationNode(obj: Any, progress: SerialisationProgression): DelegatingSerializerNode

    def getDeserializationNode(in: DeserialisationInputStream, progress: DeserialisationProgression): DeserializerNode

    def addPersistence(persistence: ObjectPersistor[_], classes: Seq[HandledClass]): Unit

    def addSerializer(persistence: ObjectPersistor[_]): Unit = addPersistence(persistence, persistence.handledClasses)

}
