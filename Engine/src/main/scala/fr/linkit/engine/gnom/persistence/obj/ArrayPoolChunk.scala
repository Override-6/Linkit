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

package fr.linkit.engine.gnom.persistence.obj

import fr.linkit.api.gnom.persistence.obj.PoolObject
import fr.linkit.engine.gnom.persistence.serial.read.DeserializerObjectPool

import java.lang.reflect.{Array => RArray}

class ArrayPoolChunk(tag: Byte, pool: ObjectPool, length: Int) extends PoolChunk[AnyRef](tag, false, pool, length) {

    //determine if the chunk is in a DeserializerObjectPool.
    private val isDeserializing = pool.isInstanceOf[DeserializerObjectPool]

    override def add(t: AnyRef): Unit = {
        if (isDeserializing) {
            super.add(t)
            return
        }
        val array = t match {
            case po: PoolObject[AnyRef] => po.value
            case array => array
        }
        val compType = array.getClass.componentType()
        if (compType == null)
            throw new IllegalArgumentException("Argument is not an array.")
        val len  = RArray.getLength(array)
        val copy = RArray.newInstance(compType, len)
        System.arraycopy(array, 0, copy, 0, len)
        super.add(copy)
        buffMap.put(System.identityHashCode(array), position())
    }

    override def indexOf(t: Any): Int = super.indexOf(t)
}