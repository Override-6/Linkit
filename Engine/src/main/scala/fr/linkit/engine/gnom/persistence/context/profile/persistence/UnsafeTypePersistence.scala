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

import fr.linkit.api.gnom.persistence.context.{ControlBox, TypePersistence}
import fr.linkit.engine.gnom.persistence.context.structure.ClassObjectStructure
import fr.linkit.engine.internal.manipulation.creation.ObjectCreator

import java.lang.reflect.Field

class UnsafeTypePersistence[T](override val structure: ClassObjectStructure) extends TypePersistence[T]() {

    private final val fields: Array[Field] = structure.fields

    def this(clazz: Class[_]) = {
        this(ClassObjectStructure(clazz))
    }

    override def initInstance(instance: T, args: Array[Any], box: ControlBox): Unit = {
        ObjectCreator.pasteAllFields(instance, fields, args.asInstanceOf[Array[AnyRef]])
    }

    override def toArray(t: T): Array[Any] = {
        ObjectCreator.getAllFields(t, fields).asInstanceOf[Array[Any]]
    }
}