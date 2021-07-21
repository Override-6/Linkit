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

package fr.linkit.engine.connection.packet.persistence.v3.persistor

import fr.linkit.api.connection.packet.persistence.tree.SerializableClassDescription
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.SerializerNode
import fr.linkit.api.connection.packet.persistence.v3.{HandledClass, ObjectPersistor, PersistenceContext}
import fr.linkit.engine.connection.packet.persistence.tree.LengthSign
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.NullInstanceNode

class DefaultObjectPersistor extends ObjectPersistor[AnyRef] {

    override val handledClasses: Seq[HandledClass] = Seq(HandledClass(classOf[Object], true))

    override def getSerialNode(obj: AnyRef, desc: SerializableClassDescription, context: PersistenceContext, progress: SerialisationProgression): SerializerNode = {
        if (obj == null || obj == None)
            return new NullInstanceNode(obj == None)

        LengthSign.of(obj, desc, context).getNode
    }

    override def placeDeserialNode(): Unit = ???
}
