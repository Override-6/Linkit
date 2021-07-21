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

import fr.linkit.api.connection.packet.persistence.tree.SerializableClassDescription
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.{DelegatingSerializerNode, SerializerNode}
import fr.linkit.api.connection.packet.persistence.v3.{HandledClass, PersistenceContext, ObjectPersistor}
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.NullInstanceNode

import scala.collection.mutable

class DefaultPersistenceContext extends PersistenceContext {

    private val persistors = mutable.HashMap.empty[String, (ObjectPersistor[Any], HandledClass)]

    override def getNode(obj: Any, desc: SerializableClassDescription, parent: SerializerNode, progress: SerialisationProgression): DelegatingSerializerNode = {
        if (obj == null || obj == None)
            return DelegatingSerializerNode(new NullInstanceNode(obj == None))

        val className = obj.getClass.getName
        persistors.get(className)
                .getOrElse()
    }

    override def addPersistence(persistence: ObjectPersistor[_], classes: Seq[HandledClass]): Unit = {
        classes.foreach(cl => persistors.put(cl.className, (persistence.asInstanceOf[ObjectPersistor[Any]], cl)))
    }

    private def getPersistence(clazz: Class[_]): ObjectPersistor[Any] = {
        if (clazz == null)
            return new NullPersistor()
        val persistor = persistors.getOrElse(clazz.getName, getPersistence(clazz.getSuperclass))
    }
}
