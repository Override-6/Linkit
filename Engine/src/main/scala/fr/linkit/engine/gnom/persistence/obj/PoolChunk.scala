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
import fr.linkit.api.gnom.persistence.obj.PoolObject
import fr.linkit.engine.gnom.persistence.obj.PoolChunk.BuffSteps
import org.jetbrains.annotations.NotNull

import java.util
import scala.reflect.ClassTag

class PoolChunk[T](val tag: Byte,
                   pool: ObjectPool,
                   maxLength: Int)(implicit cTag: ClassTag[T]) extends Freezable {

    private final var buff    = new Array[T](pool.determineBuffLength(maxLength, BuffSteps))
    private final val buffMap = new util.HashMap[Int, Int]() //Buff item Identity Hash Code -> Buff Pos + 1
    private final var pos     = 0

    private final var frozen = false

    @inline
    override def isFrozen: Boolean = frozen || pool.isFrozen

    override def freeze(): Unit = {
        frozen = true
        pos = buff.length
    }

    def resetPos(): Unit = pos = 0

    def array: Array[T] = buff

    def add(t: T): Unit = {
        if (t == null)
            throw new NullPointerException("Can't add null item")
        //if (isFrozen)
        //    throw new IllegalStateException("Could not add item in chunk: This chunk (or its pool) is frozen !")
        if (pos != 0 && pos % BuffSteps == 0) {
            if (pos >= maxLength)
                throw new IllegalStateException(s"Chunk size exceeds maxLength ('$maxLength')'")
            val extendedBuff = new Array[T](Math.min(pos + BuffSteps, maxLength))
            System.arraycopy(buff, 0, extendedBuff, 0, pos)
            buff = extendedBuff
        }
        buff(pos) = t
        t match {
            case obj: PoolObject[AnyRef] =>
                buffMap.put(obj.identity, pos + 1)
            case obj: AnyRef             =>
                buffMap.put(System.identityHashCode(obj), pos + 1)
        }
        pos += 1
    }

    def addIfAbsent(t: T): Unit = {
        if (indexOf(t) < 0)
            add(t)
    }

    @NotNull
    def get(i: Int): T = {
        if (i >= pos)
            throw new IndexOutOfBoundsException(s"$i >= $pos")
        val r = buff(i)
        if (r == null)
            throw new NullPointerException(s"Chunk '$tag' returned null item. (at index ${i})")
        r
    }

    def indexOf(t: Any): Int = {
        if (t == null)
            return -1
        buffMap.get(System.identityHashCode(t)) - 1
    }

    def size: Int = pos

}

object PoolChunk {

    private final val BuffSteps = 200
}
