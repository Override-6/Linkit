package fr.linkit.engine.connection.packet.persistence.serializor.write


class PoolChunk[@specialized() T] {

    private val objects = new Array[T](Char.MaxValue)
    private var pos     = 0

    def add(t: T): Unit = {
        objects(pos) = t
        pos += 1
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
