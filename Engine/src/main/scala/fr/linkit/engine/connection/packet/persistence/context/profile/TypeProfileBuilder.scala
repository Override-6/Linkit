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

package fr.linkit.engine.connection.packet.persistence.context.profile

import fr.linkit.api.connection.packet.persistence.context._
import fr.linkit.engine.local.utils.ClassMap

import scala.collection.mutable.ListBuffer
import scala.reflect.{ClassTag, classTag}

class TypeProfileBuilder[T <: AnyRef](implicit tag: ClassTag[T]) {

    private val persistors = ListBuffer.empty[TypePersistence[T]]
    private val convertors = new ClassMap[ObjectConverter[_ <: T, _]]

    def addPersistence(persistence: TypePersistence[T]): this.type = {
        persistors += persistence
        this
    }

    def setTConverter[B <: Any](converter: ObjectConverter[T, B]): this.type = {
        convertors.put(tag.runtimeClass, converter)
        this
    }

    def setTNewConverter[B](fTo: T => B)(fFrom: B => T): this.type = {
        setTConverter[B](new ObjectConverter[T, B] {
            override def to(t: T): B = fTo(t)

            override def from(b: B): T = fFrom(b)
        })
    }



    def setSubTConverter[S <: T : ClassTag, B](converter: ObjectConverter[S, B]): this.type = {
        setSubTConverter0(converter)
    }

    def setSubTConverter[S <: T : ClassTag, B](fTo: S => B)(fFrom: B => S): this.type = {
        setSubTConverter0[S, B](new ObjectConverter[S, B] {
            override def to(t: S): B = fTo(t)

            override def from(b: B): S = fFrom(b)
        })
        this
    }

    private def setSubTConverter0[S <: T : ClassTag, B](converter: ObjectConverter[S, B]): this.type = {
        convertors.put(classTag[S].runtimeClass, converter)
        this
    }


    def build(store: TypeProfileStore): TypeProfile[T] = {
        val clazz = tag.runtimeClass
        val parentProfile = if (clazz eq classOf[Object]) null else store.getProfile[T](clazz.getSuperclass)
        new DefaultTypeProfile[T](clazz, parentProfile, persistors.toArray, convertors.asInstanceOf[ClassMap[ObjectConverter[_ <: T, Any]]])
    }

}

object TypeProfileBuilder {
    @inline
    implicit def autoBuild[T <: AnyRef](store: TypeProfileStore, builder: TypeProfileBuilder[T]): TypeProfile[T] = {
        builder.build(store)
    }
}
