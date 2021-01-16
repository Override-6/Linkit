package fr.`override`.linkit.api.utils.cache

trait SharedCache {

    @volatile var autoFlush: Boolean

    def flush(): this.type

    def modificationCount(): Int

}
