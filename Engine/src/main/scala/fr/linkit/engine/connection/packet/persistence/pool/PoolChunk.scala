package fr.linkit.engine.connection.packet.persistence.pool

class PoolChunk[@specialized() T](buffLength: Char) {

    private val objects = new Array[T](buffLength)
    private var pos     = 0

    def array: Array[T] = objects

    @inline
    def add(t: T): Unit = {
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

    def size: Char = pos.toChar
}
