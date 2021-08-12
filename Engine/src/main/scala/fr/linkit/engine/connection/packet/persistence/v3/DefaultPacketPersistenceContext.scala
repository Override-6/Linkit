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

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.packet.persistence.v3._
import fr.linkit.api.connection.packet.persistence.v3.procedure.{FieldCompleter, MiniPersistor, Procedure}
import fr.linkit.engine.connection.packet.persistence.v3.persistor._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.{ClassTag, classTag}

class DefaultPacketPersistenceContext extends PacketPersistenceContext {

    private val persistors   = mutable.HashMap.empty[String, (ObjectPersistor[Any], HandledClass)]
    private val descriptions = mutable.HashMap.empty[String, PersistenceClassDescription[_]]

    override def putPersistor(persistence: ObjectPersistor[_], classes: Seq[HandledClass]): Unit = {
        classes.foreach(cl => persistors.put(cl.className, (persistence.asInstanceOf[ObjectPersistor[Any]], cl)))
    }

    override def getDescription[A](clazz: Class[_]): SerializableClassDescription[A] = {
        getDescription0(clazz)
    }

    override def putProcedure[A: ClassTag](procedure: Procedure[A]): Unit = {
        getDescription0[A](classTag[A].runtimeClass).setProcedure(procedure)
    }

    override def putMiniPersistor[A: ClassTag](miniPersistor: MiniPersistor[A, _]): Unit = {
        getDescription0[A](classTag[A].runtimeClass).setMiniPersistor(miniPersistor)
    }

    override def putFieldCompleter[A: ClassTag](completer: FieldCompleter[A, _]): Unit = {
        ??? //TODO
    }

    def getPersistenceForSerialisation[A](clazz: Class[_]): ObjectPersistor[A] = {
        println(s"Getting node to serialize a ${clazz.getName}. (${clazz.getName.hashCode})")
        getPersistence(clazz, SerialisationMethod.Serial)
    }

    def getPersistenceForDeserialisation[A](clazz: Class[_]): ObjectPersistor[A] = {
        println(s"Getting node to deserialize a ${clazz.getName}. (${clazz.getName.hashCode})")
        getPersistence(clazz, SerialisationMethod.Deserial)
    }

    /*private def getAllDescriptions[A](clazz: Class[_]): Iterable[PersistenceClassDescription[_ >: A]] = {
        val buffer = ListBuffer.empty[PersistenceClassDescription[_ >: A]]

        def recursive(cl: Class[_]): Unit = {
            cl.getInterfaces.foreach(i => {
                buffer += getDescription0(i)
                recursive(i)
            })
        }

        var superClass = clazz
        while (superClass != null) {
            recursive(superClass)
            superClass = superClass.getSuperclass
        }
        buffer
    }*/

    private def getPersistence[A](clazz: Class[_], method: SerialisationMethod): ObjectPersistor[A] = {
        //FIXME this is a fast fix for Persistor priority
        if (classOf[SynchronizedObject[_]].isAssignableFrom(clazz))
            return persistors(classOf[SynchronizedObject[_]].getName)._1.asInstanceOf[ObjectPersistor[A]]

        @inline
        def findPersistor(cl: Class[_]): ObjectPersistor[_] = {
            val opt = persistors.get(cl.getName)
            if (opt.exists(p => (p._2.extendedClassEnabled || (cl eq clazz)) && p._2.methods.contains(method) && p._1.willHandleClass(clazz)))
                opt.map(_._1).orNull
            else null
        }

        def getPersistorRecursively(cl: Class[_]): ObjectPersistor[_] = {
            var superClass = cl
            while (superClass != null) {
                var result = findPersistor(superClass)
                if (result != null)
                    return result
                for (interface <- superClass.getInterfaces) {
                    result = getPersistorRecursively(interface)
                    if (result != DefaultObjectPersistor)
                        return result
                }
                superClass = superClass.getSuperclass
            }
            DefaultObjectPersistor
        }

        getPersistorRecursively(clazz).asInstanceOf[ObjectPersistor[A]]
    }

    private def getDescription0[A](clazz: Class[_]): PersistenceClassDescription[A] = {
        descriptions.getOrElseUpdate(clazz.getName, new PersistenceClassDescription(clazz, this))
                .asInstanceOf[PersistenceClassDescription[A]]
    }

    putPersistor(IterablePersistor)
    putPersistor(ScalaMapPersistor)
    putPersistor(JavaCollectionPersistor)
    putPersistor(JavaMapPersistor)
}

object DefaultPacketPersistenceContext {

}
