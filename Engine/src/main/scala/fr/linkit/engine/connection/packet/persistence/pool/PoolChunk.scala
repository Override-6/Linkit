package fr.linkit.engine.connection.packet.persistence.pool

import fr.linkit.api.connection.packet.persistence.Freezable

import scala.reflect.ClassTag

class PoolChunk[@specialized() T](val tag: Byte, freezable: Freezable, buffLength: Int)(implicit cTag: ClassTag[T]) extends Freezable {

    private val objects = new Array[T](buffLength)
    private var pos     = 0

    private var frozen = false

    override def isFrozen: Boolean = frozen || freezable.isFrozen
    override def freeze(): Unit = frozen = true

    def array: Array[T] = objects

    @inline
    def add(t: T): Unit = {
        if (frozen)
            throw new IllegalStateException("Could not add item in chunk: This chunk (or its pool) is frozen !")
        objects(pos) = t
        pos += 1
    }

    def addIfAbsent(t: T): Unit = {
        if (indexOf(t) < 0)
            add(t)
    }

    def get(i: Int): T = {
        if (i >= pos)
            throw new IndexOutOfBoundsException(s"$i >= $pos")
        objects(i)
    }

    def indexOf(t: T): Int = {
        var i = 0
        while (i <= pos && i <= Char.MaxValue) {
            val registered = objects(i)
            if (registered != null && (registered == t))
                return i
            i += 1
        }
        -1
    }

    def size: Int = pos
}
