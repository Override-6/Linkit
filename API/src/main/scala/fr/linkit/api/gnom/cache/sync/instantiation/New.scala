/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.cache.sync.instantiation

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.cache.sync.instantiation.New.getAssignableConstructor
import fr.linkit.api.internal.util.Unwrapper

import java.lang.reflect.{Modifier, Constructor => JConstructor}
import scala.reflect.{ClassTag, classTag}

/**
 * SyncInstance Creator to simulate a "new A(args)" instruction.<br>
 * The creator will instantiate a new connected `A` object using a constructor.
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

    private[linkit] def getAssignableConstructor[T](clazz: Class[T], objectsArray: Array[Any]): JConstructor[T] = {
        for (constructor <- clazz.getDeclaredConstructors) {
            if (isAssignable(objectsArray, constructor)) {
                val mods = constructor.getModifiers
                if (Modifier.isPrivate(mods) || Modifier.isProtected(mods))
                    throw new IllegalArgumentException("Provided method objects structure matches a non public constructor")
                return constructor.asInstanceOf[JConstructor[T]]
            }
        }
        throw new NoSuchMethodException(s"Could not find a constructor matching arguments ${objectsArray.mkString("Array(", ", ", ")")}")
    }

    private def isAssignable(args: Array[Any], constructor: JConstructor[_]): Boolean = {
        val params = constructor.getParameterTypes
        if (params.length != args.length)
            return false

        for (i <- args.indices) {
            val arg = args(i)
            if (arg != null && !(params(i).isAssignableFrom(arg.getClass) || params(i).isAssignableFrom(Unwrapper.getPrimitiveClass(arg))))
                return false
        }
        true
    }

}
