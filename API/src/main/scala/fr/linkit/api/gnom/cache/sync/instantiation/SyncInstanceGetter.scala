package fr.linkit.api.gnom.cache.sync.instantiation

import fr.linkit.api.gnom.cache.sync.SynchronizedObject

trait SyncInstanceGetter[T<: AnyRef] {

    val tpeClass: Class[_]

    def getInstance(syncClass: Class[T with SynchronizedObject[T]]): T with SynchronizedObject[T]

}
