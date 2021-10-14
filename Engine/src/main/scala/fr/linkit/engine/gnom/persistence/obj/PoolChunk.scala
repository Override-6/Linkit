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

package fr.linkit.engine.gnom.persistence.obj

import fr.linkit.api.gnom.persistence.Freezable
import fr.linkit.engine.gnom.persistence.obj.PoolChunk.BuffSteps

import scala.reflect.ClassTag

class PoolChunk[@specialized() T](val tag: Byte,
                                  freezable: Freezable,
                                  maxLength: Int)(implicit cTag: ClassTag[T]) extends Freezable {

    private var buff = new Array[T](if (maxLength < BuffSteps) maxLength else BuffSteps)
    private var pos  = 0

    private var frozen = false

    @inline
    override def isFrozen: Boolean = frozen || freezable.isFrozen

    override def freeze(): Unit = {
        frozen = true
        pos = buff.length
    }

    def array: Array[T] = buff

    def add(t: T): Unit = {
        if (t == null)
            throw new NullPointerException("Can't add null item")
        if (isFrozen)
            throw new IllegalStateException("Could not add item in chunk: This chunk (or its pool) is frozen !")
        if (pos != 0 && pos % BuffSteps == 0) {
            if (pos >= maxLength)
                throw new IllegalStateException(s"Chunk size exceeds maxLength ('$maxLength')'")
            val extendedBuff = new Array[T](Math.min(pos + BuffSteps, maxLength))
            System.arraycopy(buff, 0, extendedBuff, 0, pos)
            buff = extendedBuff
        }
        buff(pos) = t
        pos += 1
    }

    def addIfAbsent(t: T): Unit = {
        if (indexOf(t) < 0)
            add(t)
    }

    def get(i: Int): T = {
        if (i >= pos)
            throw new IndexOutOfBoundsException(s"$i >= $pos")
        buff(i)
    }

    def indexOf(t: Any): Int = {
        if (t == null)
            return -1
        var i = 0
        while (i < pos) {
            if (buff(i) == t)
                return i
            i += 1
        }
        -1
    }

    def size: Int = pos

}

object PoolChunk {

    val BuffSteps = 200
}
