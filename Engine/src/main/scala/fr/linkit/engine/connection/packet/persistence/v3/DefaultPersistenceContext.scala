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

import fr.linkit.api.connection.packet.persistence.v3.{HandledClass, ObjectPersistor, PersistenceContext, SerializableClassDescription}
import fr.linkit.engine.connection.packet.persistence.v3.persistor.{DefaultObjectPersistor, SequencePersistor}

import scala.collection.mutable

class DefaultPersistenceContext extends PersistenceContext {

    private val persistors   = mutable.HashMap.empty[String, (ObjectPersistor[Any], HandledClass)]
    private val descriptions = mutable.HashMap.empty[String, SerializableClassDescription]

    override def addPersistence(persistence: ObjectPersistor[_], classes: Seq[HandledClass]): Unit = {
        classes.foreach(cl => persistors.put(cl.className, (persistence.asInstanceOf[ObjectPersistor[Any]], cl)))
    }

    override def getDescription(clazz: Class[_]): SerializableClassDescription = {
        descriptions.getOrElseUpdate(clazz.getName, new ClassDescription(clazz))
    }

    override def getPersistence(clazz: Class[_]): ObjectPersistor[Any] = {
        var superClass = clazz
        @inline def makeLoop(): ObjectPersistor[Any] = {
            val interfaces = superClass.getInterfaces
            for (interface <- interfaces) {
                val opt = persistors.get(interface.getName)
                if (opt.exists(_._1.willHandleClass(clazz)))
                    return opt.get._1
            }
            val opt = persistors.get(superClass.getName)
            if ((clazz eq superClass) || opt.exists(p => p._2.extendedClassEnabled && p._1.willHandleClass(clazz)))
                opt.map(_._1).orNull
            else null
        }
        while (superClass != null) {
            val result = makeLoop()
            if (result != null)
                return result
            superClass = superClass.getSuperclass
        }
        new DefaultObjectPersistor
    }

    addPersistence(new SequencePersistor)

}

object DefaultPersistenceContext {

    private val ObjectHandledClass = new HandledClass(classOf[Object].getName, true)
}
