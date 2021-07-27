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
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.SimpleObjectDeserializerNode
import fr.linkit.engine.connection.packet.persistence.v3.helper.ArrayPersistence
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.SimpleObjectSerializerNode
import fr.linkit.engine.local.utils.{JavaUtils, ScalaUtils}

import scala.collection.{IterableOps, MapFactory, MapOps, mutable}

object ScalaMapPersistor extends ObjectPersistor[collection.Map[_, _]] {

    override val handledClasses: Seq[HandledClass] = Seq(HandledClass(classOf[collection.Map[_, _]], true, Seq(SerialisationMethod.Serial, SerialisationMethod.Deserial)))

    type AnyConstructor[X] = Any
    type V[_, _] <: IterableOps[_, AnyConstructor, _]
    type CC[A, B] <: MapOps[A, Map[_, _], V, _]
    private val companions = mutable.HashMap.empty[Class[_], Option[MapFactory[CC]]]

    override def willHandleClass(clazz: Class[_]): Boolean = {
        findFactoryCompanion(clazz).isDefined
    }

    override def getSerialNode(obj: collection.Map[_, _], desc: SerializableClassDescription, context: PacketPersistenceContext, progress: SerialisationProgression): ObjectSerializerNode = {
        val node = ArrayPersistence.serialize(obj.iterator.toArray, progress)
        SimpleObjectSerializerNode(out => {
            out.writeClass(obj.getClass)
            node.writeBytes(out)
        })
    }

    override def getDeserialNode(desc: SerializableClassDescription, context: PacketPersistenceContext, progress: DeserializationProgression): ObjectDeserializerNode = {
        //TODO support sequences even if no factory is not found.
        val builder = findFactoryCompanion(desc.clazz)
                .getOrElse(throw new UnsupportedOperationException(s"factory not found for seq ${desc.clazz.getName}"))
                .newBuilder[AnyRef, AnyRef]
        val ref     = builder.result()
        SimpleObjectDeserializerNode(ref.asInstanceOf[AnyRef]) { in =>
            val content = in.readArray[(AnyRef, AnyRef)]()
            builder.addAll(content)
            val result = builder.result()
            if (!JavaUtils.sameInstance(result, ref))
                ScalaUtils.pasteAllFields(ref, result)
            ref
        }
    }

    private def findFactoryCompanion(clazz: Class[_]): Option[MapFactory[CC]] = {
        companions.getOrElseUpdate(clazz, findFactory(clazz))
    }

    private def findFactory(seqType: Class[_]): Option[MapFactory[CC]] = {
        try {
            val companionClass = Class.forName(seqType.getName + "$")
            val companion      = companionClass.getField("MODULE$").get(null)
            companion match {
                case e: MapFactory[CC] => Option(e)
                case _                 => None
            }
        } catch {
            case _: ClassNotFoundException => None
        }
    }
}
