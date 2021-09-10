package fr.linkit.engine.connection.packet.persistence.pool

import fr.linkit.api.connection.packet.persistence.Freezable
import fr.linkit.engine.connection.packet.persistence.pool.PoolChunk.BuffSteps

import scala.reflect.ClassTag

class PoolChunk[@specialized() T](val tag: Byte,
                                  freezable: Freezable,
                                  maxLength: Int)(implicit cTag: ClassTag[T]) extends Freezable {

    private var buff = new Array[T](if (maxLength < BuffSteps) maxLength else BuffSteps)
    private var pos     = 0

    private var frozen = false

    override def isFrozen: Boolean = frozen || freezable.isFrozen
    override def freeze(): Unit = {
        frozen = true
        pos = buff.length
    }

    def array: Array[T] = buff

    @inline
    def add(t: T): Unit = {
        if (frozen)
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

    def indexOf(t: T): Int = {
        var i = 0
        while (i <= pos && i <= Char.MaxValue) {
            val registered = buff(i)
            if (registered != null && (registered == t))
                return i
            i += 1
        }
        -1
    }

    def size: Int = pos

}

object PoolChunk {
    private val BuffSteps = 200
}
