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

import fr.linkit.api.gnom.persistence.Freezable
import fr.linkit.api.gnom.persistence.obj.PoolObject
import fr.linkit.engine.gnom.persistence.obj.PoolChunk.BuffSteps
import org.jetbrains.annotations.NotNull

import java.util
import scala.reflect.ClassTag

class PoolChunk[T](val tag: Byte,
                         val useHashCode: Boolean, //true to bind items with their .hashCode, false to bind with their identity hash code
                         pool: ObjectPool,
                         length: Int) //-1 if no limit
                        (implicit cTag: ClassTag[T]) extends Freezable {

    private var buff          = new Array[T](if (length < 0) BuffSteps else length)
    protected final val buffMap = new util.HashMap[Int, Int]() //Buff item Hash Code -> Buff Pos
    private var pos           = 0

    private var frozen = false

    override def isFrozen: Boolean = frozen || pool.isFrozen

    override def freeze(): Unit = {
        frozen = true
        pos = buff.length
    }

    protected def position() = pos

    def resetPos(): Unit = pos = 0

    def array: Array[T] = buff

    def add(t: T): Unit = {
        if (t == null)
            throw new NullPointerException("Can't add null item")
        resize()
        buff(pos) = t
        if (useHashCode) {
            buffMap.put(t.hashCode(), pos)
        } else t match {
            case obj: PoolObject[AnyRef] =>
                buffMap.put(obj.identity, pos)
            case obj: AnyRef             =>
                buffMap.put(System.identityHashCode(obj), pos)
        }
        pos += 1

    }

    protected def resize(): Unit = {
        val pos = this.pos
        if (pos != 0 && pos % BuffSteps == 0) {
            if (length > 0 && pos >= length)
                throw new IllegalStateException(s"Chunk size exceeds maxLength ('$length')'")
            val newSize      = pos + BuffSteps
            val extendedBuff = if (length < 0) new Array[T](newSize) else new Array[T](Math.min(newSize, length))
            System.arraycopy(buff, 0, extendedBuff, 0, pos)
            buff = extendedBuff
        }
    }

    def addIfAbsent(t: T): Int = {
        val idx = indexOf(t)
        if (idx < 0) {
            add(t)
        }
        idx
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
        if (useHashCode) buffMap.get(t.hashCode()) - 1
        else buffMap.get(System.identityHashCode(t)) - 1
    }

    def size: Int = pos

}

object PoolChunk {

    @inline
    private final val BuffSteps = 200
}
