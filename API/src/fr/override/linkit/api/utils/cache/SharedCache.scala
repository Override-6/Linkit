package fr.`override`.linkit.api.utils.cache

trait SharedCache extends AutoCloseable {

    var autoFlush: Boolean

    def flush(): this.type

    def modificationCount(): Int

}
