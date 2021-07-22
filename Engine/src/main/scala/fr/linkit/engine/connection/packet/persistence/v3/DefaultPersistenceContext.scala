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

package fr.linkit.engine.connection.packet.persistence.v3

import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.DeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.{DeserialisationInputStream, DeserialisationProgression}
import fr.linkit.api.connection.packet.persistence.v3.serialisation.{SerialisationOutputStream, SerialisationProgression}
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.{DelegatingSerializerNode, SerializerNode}
import fr.linkit.api.connection.packet.persistence.v3.{HandledClass, ObjectPersistor, PersistenceContext, SerializableClassDescription}
import fr.linkit.engine.connection.packet.persistence.v3.DefaultPersistenceContext.ObjectHandledClass
import fr.linkit.engine.connection.packet.persistence.v3.persistor.DefaultObjectPersistor
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.SerializerNodeFlags._
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.NullInstanceNode
import fr.linkit.engine.local.mapping.{ClassMappings, ClassNotMappedException}
import fr.linkit.engine.local.utils.{NumberSerializer, UnWrapper}

import scala.collection.mutable

class DefaultPersistenceContext extends PersistenceContext {

    private val persistors   = mutable.HashMap.empty[String, (ObjectPersistor[Any], HandledClass)]
    private val descriptions = mutable.HashMap.empty[String, SerializableClassDescription]

    override def getSerializationNode(obj: Any, out: SerialisationOutputStream, progress: SerialisationProgression): DelegatingSerializerNode = {
        val node: SerializerNode = obj match {
            case null | None                          => new NullInstanceNode(obj == None)
            case i if UnWrapper.isPrimitiveWrapper(i) => out.writePrimitive(i.asInstanceOf[AnyVal])
            case e: Enum[_]                           => out.writeEnum(e)
            case str: String                          => out.writeString(str)
            case array: Array[Any]                    => out.writeArray(array)
            case _                                    =>
                val clazz = obj.getClass
                val desc  = getDesc(clazz)
                return progress.checkNode(obj) {
                    getPersistence(clazz)
                            .getSerialNode(obj, desc, this, progress)
                }
        }
        DelegatingSerializerNode(node)
    }

    override def getDeserializationNode(in: DeserialisationInputStream, progress: DeserialisationProgression): DeserializerNode = {
        val buff = in.buff
        val pos = buff.position()
        buff.get(pos) match {
            case b if b >= ByteFlag && b <= BooleanFlag => _.readPrimitive()
            case StringFlag                             => _.readString()
            case ArrayFlag                              => _.readArray()
            case HeadedObjectFlag                       => progress.getHeaderObjectNode(NumberSerializer.deserializeFlaggedNumber(in))
            case _                                      =>
                //buff.position(buff.position() - 1) //for object, no flag is set, the first byte is a member of the object type int code, so we need to make a rollback in order to integrate the first byte.
                val classCode = buff.getInt
                val objectClass = ClassMappings.getClass(classCode)
                if (objectClass == null)
                    throw new ClassNotMappedException(s"classCode $classCode is not mapped.")
                if (objectClass.isEnum)
                    _.readEnum()
                else getPersistence(objectClass).getDeserialNode(getDesc(objectClass), this, progress)
        }
    }

    override def addPersistence(persistence: ObjectPersistor[_], classes: Seq[HandledClass]): Unit = {
        classes.foreach(cl => persistors.put(cl.className, (persistence.asInstanceOf[ObjectPersistor[Any]], cl)))
    }

    private def getDesc(clazz: Class[_]): SerializableClassDescription = {
        descriptions.getOrElse(clazz.getName, new ClassDescription(clazz))
    }

    private def getPersistence(clazz: Class[_]): ObjectPersistor[Any] = {
        def getPersistenceRecursively(superClass: Class[_]): (ObjectPersistor[Any], HandledClass) = {
            if (superClass == null)
                return (new DefaultObjectPersistor(), ObjectHandledClass)
            val (persistor, handledClass) = persistors.getOrElse(clazz.getName, getPersistenceRecursively(superClass.getSuperclass)): (ObjectPersistor[Any], HandledClass)
            if ((clazz eq superClass) || handledClass.extendedClassEnabled)
                (persistor, handledClass)
            else getPersistenceRecursively(superClass.getSuperclass)
        }

        getPersistenceRecursively(clazz)._1
    }

}

object DefaultPersistenceContext {

    private val ObjectHandledClass = new HandledClass(classOf[Object].getName, true)
}
