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

import fr.linkit.engine.local.utils.ScalaUtils

import java.lang.reflect.{Field, Modifier}

class UnsafeTypeProfile[T](clazz: Class[_]) extends AbstractTypeProfile[T](clazz) {

    private val fields: Array[Field] = ScalaUtils.retrieveAllFields(clazz).filterNot(f => Modifier.isTransient(f.getModifiers))

    override def completeInstance(instance: T, args: Array[Any]): T = {
        val fields = this.fields
        for (i <- args.indices) {
            ScalaUtils.setValue(instance, fields(i), args(i))
        }
        instance
    }

    override def toArray(t: T): Array[Any] = {
        val buff = new Array[Any](fields.length)
        for (i <- fields.indices) {
            buff(i) = fields(i).get(t)
        }
        buff
    }
}

object UnsafeTypeProfile {

    private val Unsafe = ScalaUtils.findUnsafe()
}
