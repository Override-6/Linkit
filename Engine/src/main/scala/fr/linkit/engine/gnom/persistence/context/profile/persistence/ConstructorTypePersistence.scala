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

package fr.linkit.engine.gnom.persistence.context.profile.persistence

import java.lang.reflect.Constructor
import fr.linkit.api.gnom.persistence.context.{ControlBox, Deconstructor, TypePersistence}
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.engine.gnom.persistence.context.Persist
import fr.linkit.engine.gnom.persistence.context.profile.persistence.ConstructorTypePersistence.getConstructor
import fr.linkit.engine.gnom.persistence.context.structure.ArrayObjectStructure
import fr.linkit.engine.internal.manipulation.invokation.ConstructorInvoker

class ConstructorTypePersistence[T <: AnyRef](clazz: Class[_], constructor: Constructor[T], deconstructor: T => Array[Any]) extends TypePersistence[T]() {

    def this(clazz: Class[_], deconstructor: Deconstructor[T]) {
        this(clazz, getConstructor[T](clazz), deconstructor.deconstruct(_: T))
    }

    def this(clazz: Class[_], constructor: Constructor[T], deconstructor: Deconstructor[T]) {
        this(clazz, constructor, deconstructor.deconstruct(_: T))
    }

    override val structure: ObjectStructure = ArrayObjectStructure(constructor.getParameterTypes: _*)
    private  val invoker                    = new ConstructorInvoker(constructor)

    override def initInstance(allocatedObject: T, args: Array[Any], box: ControlBox): Unit = {
        invoker.invoke(allocatedObject, args)
    }

    override def toArray(t: T): Array[Any] = {
        deconstructor(t)
    }

}

object ConstructorTypePersistence {

    def getConstructor[T](clazz: Class[_]): Constructor[T] = {
        findPersistConstructor(clazz)
            .getOrElse {
                throw new NoSuchElementException(s"No Constructor is annotated for $clazz, please specify a constructor in your configuration or annotate one using @${classOf[Persist].getSimpleName}")
            }.asInstanceOf[Constructor[T]]
    }

    def findPersistConstructor[T](clazz: Class[_]): Option[Constructor[T]] = {
        val opt = clazz.getDeclaredConstructors
            .find(_.isAnnotationPresent(classOf[Persist]))
            .asInstanceOf[Option[Constructor[T]]]
        if (opt.isDefined)
            opt.get.setAccessible(true)
        opt
    }
}
