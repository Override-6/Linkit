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

import fr.linkit.api.connection.packet.persistence.v3._
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserializationProgression
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.ObjectDeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.ObjectSerializerNode
import fr.linkit.engine.connection.packet.persistence.v3.{ArraySign, ClassDescription}
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.SimpleObjectDeserializerNode
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.{NullInstanceNode, SimpleObjectSerializerNode}
import fr.linkit.engine.local.utils.ScalaUtils

object DefaultObjectPersistor extends ObjectPersistor[Any] {

    override val handledClasses: Seq[HandledClass] = Seq(HandledClass(classOf[Object], true, Seq(SerialisationMethod.Deserial, SerialisationMethod.Serial)))

    override def getSerialNode(obj: Any, desc: SerializableClassDescription, context: PacketPersistenceContext, progress: SerialisationProgression): ObjectSerializerNode = {
        if (obj == null || obj == None)
            return new NullInstanceNode(obj == None)

        val fieldValues = desc.serializableFields
                .map(_.first.get(obj))
        val node        = ArraySign.out(fieldValues, progress).getNode
        SimpleObjectSerializerNode(out => {
            out.writeClass(obj.getClass)
            node.writeBytes(out)
        })
    }

    override def getDeserialNode(desc: SerializableClassDescription, context: PacketPersistenceContext, progress: DeserializationProgression): ObjectDeserializerNode = {
        val instance = ScalaUtils.allocate[AnyRef](desc.clazz)
        getCustomDeserialNode(instance)
    }

    def getCustomDeserialNode(instance: Any): ObjectDeserializerNode = {
        val desc = ClassDescription(instance.getClass)
        SimpleObjectDeserializerNode(instance) {
            in =>
                //println(s"Deserializing object ${desc.clazz.getName}...")
                ArraySign.in(desc.signItemCount, in).deserializeRef(instance)(nodes => {
                    desc.foreachDeserializableFields { (i, _) =>
                        nodes(i).deserialize(in)
                    } { (field, value) =>
                        ScalaUtils.setValue(instance, field, value)
                    }
                    instance
                }).deserialize(in)
        }
    }
}
