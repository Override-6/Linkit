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

import fr.linkit.api.connection.packet.persistence.v3.SerialisationMethod._
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserializationProgression
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.ObjectDeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.ObjectSerializerNode
import fr.linkit.api.connection.packet.persistence.v3.{HandledClass, ObjectPersistor, PacketPersistenceContext, SerializableClassDescription}
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.SimpleObjectDeserializerNode
import fr.linkit.engine.connection.packet.persistence.v3.helper.ArrayPersistence
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.SimpleObjectSerializerNode
import fr.linkit.engine.local.utils.ScalaUtils

import java.util

object JavaMapPersistor extends ObjectPersistor[util.Map[_, _]] {

    override val handledClasses: Seq[HandledClass] = Seq(HandledClass(classOf[util.Map[_, _]], true, Seq(Serial, Deserial)))

    override def getSerialNode(obj: util.Map[_, _], desc: SerializableClassDescription, context: PacketPersistenceContext, progress: SerialisationProgression): ObjectSerializerNode = {
        val content = obj.entrySet().toArray[util.Map.Entry[AnyRef, AnyRef]]((i: Int) => new Array[util.Map.Entry[AnyRef, AnyRef]](i))
        val node = ArrayPersistence.serialize(content, progress)
        SimpleObjectSerializerNode { out =>
            out.writeClass(obj.getClass)
            node.writeBytes(out)
        }
    }

    override def getDeserialNode(desc: SerializableClassDescription, context: PacketPersistenceContext, progress: DeserializationProgression): ObjectDeserializerNode = {
        val ref = try {
            val constructor = desc.clazz.getConstructor()
            constructor.setAccessible(true)
            constructor.newInstance()
                    .asInstanceOf[util.Map[AnyRef, AnyRef]]
        } catch {
            case _: NoSuchMethodException =>
                ScalaUtils.allocate(desc.clazz)
        }
        SimpleObjectDeserializerNode(ref) { in =>
            val content = in.readArray[util.Map.Entry[AnyRef, AnyRef]]()
            content.foreach(entry => {
                val key   = entry.getKey
                val value = entry.getValue
                ref.put(key, value)
            })
            ref
        }
    }
}
