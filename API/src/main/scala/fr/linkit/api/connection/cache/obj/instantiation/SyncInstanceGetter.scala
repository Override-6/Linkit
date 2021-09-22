package fr.linkit.api.connection.cache.obj.instantiation

import fr.linkit.api.connection.cache.obj.SynchronizedObject

trait SyncInstanceGetter[T] {

    val tpeClass: Class[_]

    def getInstance(syncClass: Class[T with SynchronizedObject[T]]): T with SynchronizedObject[T]

}
