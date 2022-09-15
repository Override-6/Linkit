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

package fr.linkit.engine.gnom.persistence.config.profile.persistence

import fr.linkit.api.gnom.persistence.context.Deconstructible.Persist
import fr.linkit.api.gnom.persistence.context.{ControlBox, Deconstructor, TypePersistence}
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.engine.gnom.persistence.config.profile.persistence.ConstructorTypePersistence.getPersistConstructor
import fr.linkit.engine.gnom.persistence.config.structure.ArrayObjectStructure
import fr.linkit.engine.internal.manipulation.invokation.ConstructorInvoker

import java.lang.reflect.Constructor

class ConstructorTypePersistence[T <: AnyRef](constructor: Constructor[T], deconstructor: T => Array[Any]) extends TypePersistence[T]() {
    
    def this(clazz: Class[_], deconstructor: Deconstructor[T]) {
        this(getPersistConstructor[T](clazz), deconstructor.deconstruct(_: T))
    }
    
    def this(constructor: Constructor[T], deconstructor: Deconstructor[T]) {
        this(constructor, deconstructor.deconstruct(_: T))
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
    
    def getPersistConstructor[T](clazz: Class[_]): Constructor[T] = {
        findPersistConstructor(clazz)
                .getOrElse {
                    throw new NoSuchElementException(s"No Constructor is annotated for $clazz, please specify a constructor in your configuration or annotate one using @${classOf[Persist].getSimpleName}")
                }.asInstanceOf[Constructor[T]]
    }
    
    def findPersistConstructor[T](clazz: Class[_]): Option[Constructor[T]] = {
        val constructors                       = clazz.getDeclaredConstructors
        var persistConstructor: Constructor[_] = null
        for (constructor <- constructors) {
            if (constructor.isAnnotationPresent(classOf[Persist])) {
                if (persistConstructor != null)
                    throw new IllegalDeconstructiveClassException(s"for class: ${clazz.getName} extending Deconstructive: There is more than one constructor annotated with @Persist.")
                persistConstructor = constructor
            }
        }
        if (persistConstructor != null)
            persistConstructor.setAccessible(true)
        Option(persistConstructor)
                .asInstanceOf[Option[Constructor[T]]]
    }
}
