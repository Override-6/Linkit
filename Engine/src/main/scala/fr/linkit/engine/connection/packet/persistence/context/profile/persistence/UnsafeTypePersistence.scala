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

import fr.linkit.api.connection.packet.persistence.context.TypePersistence
import fr.linkit.engine.connection.packet.persistence.context.structure.ClassObjectStructure
import fr.linkit.engine.local.utils.ScalaUtils

import java.lang.reflect.Field

class UnsafeTypePersistence[T](clazz: Class[_]) extends TypePersistence[T]() {

    override val structure: ClassObjectStructure = ClassObjectStructure(clazz)
    private  val fields   : Array[Field]         = structure.fields

    override def initInstance(instance: T, args: Array[Any]): Unit = {
        val fields = this.fields
        for (i <- args.indices) {
            ScalaUtils.setValue(instance, fields(i), args(i))
        }
    }

    override def toArray(t: T): Array[Any] = {
        val buff = new Array[Any](fields.length)
        for (i <- fields.indices) {
            buff(i) = ScalaUtils.getValue(t, fields(i))
        }
        buff
    }
}

object UnsafeTypePersistence {

    private val Unsafe = ScalaUtils.findUnsafe()
}
