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

package fr.linkit.engine.gnom.persistence.config

import fr.linkit.api.gnom.persistence.context.{Deconstructor, PersistenceContext}
import fr.linkit.engine.internal.util.ClassMap

import java.lang.reflect.Constructor
import scala.reflect.{ClassTag, classTag}

abstract class PersistenceContextBuilder {

    private val constructorMap = new ClassMap[Constructor[_]]()
    private val deconstructorMap = new ClassMap[Deconstructor[_]]()

    def useConstructor[T <: AnyRef : ClassTag](constructor: Constructor[T])(deconstructor: Deconstructor[T]): Unit = {
        val clazz = classTag[T].runtimeClass
        constructorMap.put(clazz, constructor)
        deconstructorMap.put(clazz, deconstructor)
    }

    def build(): PersistenceContext = ImmutablePersistenceContext(constructorMap, deconstructorMap)

}
