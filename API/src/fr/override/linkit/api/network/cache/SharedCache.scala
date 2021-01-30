package fr.`override`.linkit.api.network.cache

trait SharedCache {

    @volatile var autoFlush: Boolean

    val family: String

    def flush(): this.type

    def modificationCount(): Int

}
