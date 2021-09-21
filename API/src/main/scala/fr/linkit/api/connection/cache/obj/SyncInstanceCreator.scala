package fr.linkit.api.connection.cache.obj

trait SyncInstanceCreator[T] {

    val tpeClass: Class[_]

    def newInstance(clazz: Class[T with SynchronizedObject[T]]): T with SynchronizedObject[T]

}
