package fr.linkit.engine.gnom.persistence.serializor.read

import fr.linkit.api.gnom.persistence.obj.PoolObject

class NotInstantiatedArray[T](pool: DeserializerObjectPool, arrayContent: Array[Int], emptyArray: Array[T]) extends PoolObject[Array[T]] {

    override lazy val value: Array[T] = {
        var i = 0
        while (i < arrayContent.length) {
            val any = pool.getAny(arrayContent(i))
            if (any != null) {
                emptyArray(i) = any match {
                    case p: PoolObject[T] => p.value
                    case o: T             => o
                }
            }
            i += 1
        }
        emptyArray
    }
}
