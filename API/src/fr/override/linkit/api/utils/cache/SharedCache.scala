package fr.`override`.linkit.api.utils.cache

trait SharedCache extends {

    var autoFlush: Boolean

    def flush(): Unit

    def modificationCount(): Int

}
