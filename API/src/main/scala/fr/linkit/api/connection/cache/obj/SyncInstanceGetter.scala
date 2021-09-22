package fr.linkit.api.connection.cache.obj

trait SyncInstanceGetter[T] {

    val tpeClass: Class[_]

    def getInstance(syncClass: Class[T with SynchronizedObject[T]]): T with SynchronizedObject[T]

}
