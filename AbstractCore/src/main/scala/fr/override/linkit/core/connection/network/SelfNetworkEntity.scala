package fr.`override`.linkit.core.connection.network

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.connection.network.cache.SharedCacheManager
import fr.`override`.linkit.api.connection.network.{ConnectionState, Network, NetworkEntity}

import java.sql.Timestamp

class SelfNetworkEntity(connection: ConnectionContext,
                        override val cache: SharedCacheManager) extends NetworkEntity {

    override val identifier: String = connection.identifier

    override val connectionDate: Timestamp = new Timestamp(System.currentTimeMillis())

    override val network: Network = connection.network

    //override val apiVersion: Version = Relay.ApiVersion

    //override val relayVersion: Version = relay.relayVersion

    update()

    override def update(): this.type = {
        cache.post(2, connectionDate)
        //cache.post(4, apiVersion)
        //cache.post(5, relayVersion)
        cache.update()
        this
    }

    override def getConnectionState: ConnectionState = connection.getState

    override def toString: String = s"SelfNetworkEntity(identifier: ${identifier})"

}
