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

import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.DeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.SerializerNode
import fr.linkit.api.connection.packet.persistence.v3.{HandledClass, ObjectPersistor, PersistenceContext, SerializableClassDescription}
import fr.linkit.engine.connection.packet.persistence.v3.ArraySign
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.NullInstanceNode
import fr.linkit.engine.local.utils.ScalaUtils

class DefaultObjectPersistor extends ObjectPersistor[Any] {

    override val handledClasses: Seq[HandledClass] = Seq(HandledClass(classOf[Object], true))

    override def getSerialNode(obj: Any, desc: SerializableClassDescription, context: PersistenceContext, progress: SerialisationProgression): SerializerNode = {
        if (obj == null || obj == None)
            return new NullInstanceNode(obj == None)

        val fieldValues = desc.serializableFields
                .map(_.first.get(obj))
        out => {
            out.writeClass(obj.getClass)
            ArraySign.out(fieldValues, out, progress, context).getNode.writeBytes(out)
        }
    }

    override def getDeserialNode(desc: SerializableClassDescription, context: PersistenceContext, progress: DeserialisationProgression): DeserializerNode = {
        in =>
            val instance = ScalaUtils.allocate[AnyRef](desc.clazz)
            println(s"Deserializing object ${desc.clazz.getName}...")
            ArraySign.in(desc.signItemCount, context, progress, in).getNode(nodes => {
                desc.foreachDeserializableFields((i, field) => {
                    val obj = nodes(i).getObject(in)
                    ScalaUtils.setValue(instance, field, obj)
                })
                instance
            }).getObject(in)

    }

}
