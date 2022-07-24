/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.cache.sync.instantiation

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassDef, SyncClassDefUnique}
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceCreator
import fr.linkit.engine.gnom.cache.sync.instantiation.New.getAssignableConstructor
import fr.linkit.engine.gnom.persistence.config.structure.ArrayObjectStructure

import java.lang.reflect.{Modifier, Constructor => JConstructor}
import scala.reflect.{ClassTag, classTag}

/**
 * SyncInstance Creator to simulate a "new A(args)" instruction.<br>
 * The creator will generate a synchronized `A` object using a constructor.
 * */
class New[A <: AnyRef] private(clazz: Class[A],
                               arguments: Array[Any]) extends SyncInstanceCreator[A] {

    override val syncClassDef: SyncClassDef = SyncClassDef(clazz)

    override def getInstance(syncClass: Class[A with SynchronizedObject[A]]): A with SynchronizedObject[A] = {
        val constructor = getAssignableConstructor(syncClass, arguments)
        constructor.newInstance(arguments: _*)
    }

    override def getOrigin: Option[A] = None
}

object New {

    def apply[T <: AnyRef : ClassTag](params: Any*): New[T] = {
        val clazz        = classTag[T].runtimeClass.asInstanceOf[Class[T]]
        val objectsArray = params.toArray
        new New[T](clazz, objectsArray)
    }

    def getAssignableConstructor[T](clazz: Class[T], objectsArray: Array[Any]): JConstructor[T] = {
        for (constructor <- clazz.getDeclaredConstructors) {
            val params               = constructor.getParameterTypes
            val constructorStructure = ArrayObjectStructure(params: _*)
            if (constructorStructure.isAssignable(objectsArray)) {
                val mods = constructor.getModifiers
                if (Modifier.isPrivate(mods) || Modifier.isProtected(mods))
                    throw new IllegalArgumentException("Provided method objects structure matches a non public constructor")
                return constructor.asInstanceOf[JConstructor[T]]
            }
        }
        throw new NoSuchMethodException(s"Could not find a constructor matching arguments ${objectsArray.mkString("Array(", ", ", ")")}")
    }

}
