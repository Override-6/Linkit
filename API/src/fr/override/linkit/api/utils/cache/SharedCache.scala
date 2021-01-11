package fr.`override`.linkit.api.utils.cache

trait SharedCache extends {

    val isAutoFlush: Boolean

    def flush(): Unit

    def modificationCount(): Int

}
