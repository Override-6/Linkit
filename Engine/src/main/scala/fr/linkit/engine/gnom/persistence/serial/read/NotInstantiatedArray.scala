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

package fr.linkit.engine.gnom.persistence.serial.read

import fr.linkit.api.gnom.persistence.obj.{PoolObject, RegistrablePoolObject}

class NotInstantiatedArray[T <: AnyRef](pool: DeserializerObjectPool,
                                        arrayContent: Array[Int],
                                        emptyArray: Array[T]) extends RegistrablePoolObject[Array[T]] {

    private val contentObjects = new Array[RegistrablePoolObject[T]](arrayContent.length)

    override def register(): Unit = {
        var i = 0
        while (i < contentObjects.length) {
            val obj = contentObjects(i)
            if (obj != null)
                obj.register()
            i += 1
        }
    }

    override def identity: Int = System.identityHashCode(emptyArray)

    override lazy val value: Array[T] = {
        var i = 0
        while (i < arrayContent.length) {
            val any = pool.getAny(arrayContent(i))
            if (any != null) {
                emptyArray(i) = any match {
                    case p: RegistrablePoolObject[T] =>
                        contentObjects(i) = p
                        p.value
                    case p: PoolObject[T]            => p.value
                    case o: T                        => o
                }
            }
            i += 1
        }
        emptyArray
    }
}
