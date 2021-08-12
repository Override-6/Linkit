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

object JavaCollectionPersistor extends ObjectPersistor[util.Collection[_]] {

    override val handledClasses: Seq[HandledClass] = Seq(HandledClass(classOf[util.Collection[_]], true, Seq(Serial, Deserial)))

    override def getSerialNode(obj: util.Collection[_], desc: SerializableClassDescription[util.Collection[_]], context: PacketPersistenceContext, progress: SerialisationProgression): ObjectSerializerNode = {
        val node = ArrayPersistence.serialize(obj.toArray, progress)
        SimpleObjectSerializerNode { out =>
            out.writeClass(obj.getClass)
            node.writeBytes(out)
        }
    }

    override def getDeserialNode(desc: SerializableClassDescription[util.Collection[_]], context: PacketPersistenceContext, progress: DeserializationProgression): ObjectDeserializerNode = {
        val ref = try {
            val constructor = desc.clazz.getConstructor()
            constructor.setAccessible(true)
            constructor.newInstance()
                    .asInstanceOf[util.Collection[AnyRef]]
        } catch {
            case _: NoSuchMethodException =>
                ScalaUtils.allocate(desc.clazz)
        }
        SimpleObjectDeserializerNode(ref) { in =>
            val content = in.readArray[AnyRef]()
            //TODO handle immutable collections
            if (!content.isEmpty) {
                val contentList = util.Arrays.asList[AnyRef](content: _*)
                ref.addAll(contentList)
            }
            ref
        }
    }
}
