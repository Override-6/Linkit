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

package fr.linkit.engine.connection.packet.persistence.context.profile.persistence

import fr.linkit.api.connection.packet.persistence.context.Deconstructor
import fr.linkit.api.connection.packet.persistence.obj.ObjectStructure
import fr.linkit.engine.connection.packet.persistence.context
import fr.linkit.engine.connection.packet.persistence.context.ClassObjectStructure
import fr.linkit.engine.connection.packet.persistence.context.profile.persistence.ConstructorTypePersistence.getConstructor

import java.lang.invoke.MethodHandles
import java.lang.reflect.Constructor

class ConstructorTypePersistence[T](clazz: Class[_], constructor: Constructor[T], deconstructor: T => Array[Any]) extends AbstractTypePersistence[T]() {

    def this(clazz: Class[_], deconstructor: Deconstructor[T]) {
        this(clazz, getConstructor[T](clazz), deconstructor.deconstruct(_))
    }

    def this(clazz: Class[_], constructor: Constructor[T], deconstructor: Deconstructor[T]) {
        this(clazz, constructor, deconstructor.deconstruct(_))
    }

    override val structure: ObjectStructure = ClassObjectStructure(clazz)

    private val handle = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup()).unreflectConstructor(constructor)

    override def initInstance(allocatedObject: T, args: Array[Any]): T = {
        val result = handle.bindTo(allocatedObject).invoke(args)
        result.asInstanceOf[T]
    }

    override def toArray(t: T): Array[Any] = {
        deconstructor(t)
    }

}

object ConstructorTypePersistence {

    def getConstructor[T](clazz: Class[_]): Constructor[T] = {
        findConstructor(clazz)
                .getOrElse {
                    throw new NoSuchElementException(s"No Constructor is annotated for $clazz, please specify a constructor or annotate one using @Constructor")
                }.asInstanceOf[Constructor[T]]
    }

    def findConstructor[T](clazz: Class[_]): Option[Constructor[T]] = {
        val opt = clazz.getDeclaredConstructors
                .find(_.isAnnotationPresent(classOf[context.Constructor]))
                .asInstanceOf[Option[Constructor[T]]]
        if (opt.isDefined)
            opt.get.setAccessible(true)
        opt
    }
}
