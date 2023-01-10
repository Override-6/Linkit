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

package fr.linkit.engine.gnom.persistence.config.profile

import fr.linkit.api.gnom.persistence.context._

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

class TypeProfileBuilder[T <: AnyRef](implicit tag: ClassTag[T]) {

    private val persistors = ListBuffer.empty[TypePersistor[T]]

    def addPersistor(persistor: TypePersistor[T]): this.type = {
        persistors += persistor
        this
    }

    def build(store: TypeProfileStore): TypeProfile[T] = {
        val clazz = tag.runtimeClass
        new DefaultTypeProfile[T](clazz, store, persistors.toArray)
    }

}

object TypeProfileBuilder {
    @inline
    implicit def autoBuild[T <: AnyRef](store: TypeProfileStore, builder: TypeProfileBuilder[T]): TypeProfile[T] = {
        builder.build(store)
    }
}
