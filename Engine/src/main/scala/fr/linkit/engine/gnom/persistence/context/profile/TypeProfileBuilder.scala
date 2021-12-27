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

package fr.linkit.engine.gnom.persistence.context.profile

import fr.linkit.api.gnom.persistence.context._
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.engine.gnom.persistence.context.structure.ArrayObjectStructure
import fr.linkit.engine.internal.utils.{ClassMap, ScalaUtils}

import scala.collection.mutable.ListBuffer
import scala.reflect.{ClassTag, classTag}

class TypeProfileBuilder[T <: AnyRef](implicit tag: ClassTag[T]) {

    private val persistors = ListBuffer.empty[TypePersistence[T]]

    def addPersistence(persistence: TypePersistence[T]): this.type = {
        persistors += persistence
        this
    }


    def setTConverter[B : ClassTag](fTo: T => B)(fFrom: B => T): this.type = {
        val clazz = classTag[B].runtimeClass
        val persistor = new TypePersistence[T] {
            override val structure: ObjectStructure = new ArrayObjectStructure {
                override val types: Array[Class[_]] = Array(clazz)
            }

            override def initInstance(allocatedObject: T, args: Array[Any], box: ControlBox): Unit = {
                args.head match {
                    case t: B => ScalaUtils.pasteAllFields(allocatedObject, fFrom(t))
                }
            }

            override def toArray(t: T): Array[Any] = Array(fTo(t))
        }
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
