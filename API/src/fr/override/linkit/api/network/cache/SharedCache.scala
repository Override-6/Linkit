package fr.`override`.linkit.api.network.cache

trait SharedCache {

    @volatile var autoFlush: Boolean

    def flush(): this.type

    def modificationCount(): Int

}
