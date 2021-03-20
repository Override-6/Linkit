package fr.`override`.linkit.core.connection.network

import fr.`override`.linkit.api.connection.network.cache.SharedCacheManager
import fr.`override`.linkit.api.connection.network.{ConnectionState, NetworkEntity}

import java.sql.Timestamp

abstract class AbstractRemoteEntity(override val identifier: String,
                                    override val cache: SharedCacheManager) extends NetworkEntity {

    override def connectionDate: Timestamp = cache.getOrWait(2)

    override def update(): this.type = {
        cache.update()
        this
    }

    override def getConnectionState: ConnectionState

    override def toString: String = s"${getClass.getSimpleName}(identifier: $identifier, state: $getConnectionState)"
}
