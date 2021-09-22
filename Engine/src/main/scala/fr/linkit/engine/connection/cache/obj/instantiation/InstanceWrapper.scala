package fr.linkit.engine.connection.cache.obj.instantiation

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.instantiation.SyncInstanceGetter

class InstanceWrapper[T](obj: T with SynchronizedObject[T]) extends SyncInstanceGetter[T] {
    override val tpeClass: Class[_] = obj.getSuperClass

    override def getInstance(syncClass: Class[T with SynchronizedObject[T]]): T with SynchronizedObject[T] = {
        if (obj.getClass ne syncClass)
            throw new IllegalArgumentException(s"Required sync object type is not equals to stored sync object (${obj.getClass} / $syncClass")
        obj
    }
}
