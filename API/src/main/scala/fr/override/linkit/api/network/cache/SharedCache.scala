package fr.`override`.linkit.api.network.cache

import fr.`override`.linkit.api.network.Updatable

trait SharedCache extends Updatable {

    @volatile var autoFlush: Boolean

    val family: String

    def flush(): this.type

    def modificationCount(): Int

}
