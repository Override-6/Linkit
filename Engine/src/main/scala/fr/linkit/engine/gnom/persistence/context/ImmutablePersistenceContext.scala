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

package fr.linkit.engine.gnom.persistence.context

import fr.linkit.api.gnom.persistence.context.{Deconstructor, PersistenceContext}
import fr.linkit.engine.internal.utils.ClassMap

import java.lang.reflect.Constructor

class ImmutablePersistenceContext private(constructors: ClassMap[Constructor[_]],
                                          deconstructor: ClassMap[Deconstructor[_]]) extends PersistenceContext {


    override def findConstructor[T](clazz: Class[_]): Option[java.lang.reflect.Constructor[T]] = {
        constructors.get(clazz)
                .asInstanceOf[Option[java.lang.reflect.Constructor[T]]]
    }

    override def findDeconstructor[T](clazz: Class[_]): Option[Deconstructor[T]] = {
        deconstructor.get(clazz)
                .asInstanceOf[Option[Deconstructor[T]]]
    }

}

object ImmutablePersistenceContext {

    def apply(constructors: ClassMap[Constructor[_]], deconstructor: ClassMap[Deconstructor[_]]): ImmutablePersistenceContext = {
        new ImmutablePersistenceContext(new ClassMap(constructors), new ClassMap(deconstructor))
    }
    def apply(): ImmutablePersistenceContext = {
        new ImmutablePersistenceContext(new ClassMap(), new ClassMap())
    }
}
