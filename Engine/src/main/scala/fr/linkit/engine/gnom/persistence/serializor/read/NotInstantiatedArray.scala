package fr.linkit.engine.gnom.persistence.serializor.read

import fr.linkit.api.gnom.persistence.obj.{PoolObject, RegistrablePoolObject}

class NotInstantiatedArray[T <: AnyRef](pool: DeserializerObjectPool, arrayContent: Array[Int], emptyArray: Array[T]) extends RegistrablePoolObject[Array[T]] {

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
