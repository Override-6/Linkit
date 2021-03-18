package fr.`override`.linkit.api.connection.network.cache

import fr.`override`.linkit.api.connection.network.Updatable

trait SharedCache extends Updatable {

    @volatile var autoFlush: Boolean

    val family: String

    def flush(): this.type

    def modificationCount(): Int

}
