package fr.`override`.linkit.client.network

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.connection.network.ConnectionState
import fr.`override`.linkit.api.connection.network.cache.SharedCacheManager
import fr.`override`.linkit.core.connection.network.AbstractRemoteEntity
import fr.`override`.linkit.core.connection.network.cache.{AbstractSharedCacheManager, SharedInstance}
import fr.`override`.linkit.core.connection.packet.traffic.channel.CommunicationPacketChannel

class ConnectionNetworkEntity private(connection: ConnectionContext,
                                      identifier: String,
                                      cache: SharedCacheManager)
        extends AbstractRemoteEntity(identifier, cache) {

    def this(connection: ConnectionContext, identifier: String, communicator: CommunicationPacketChannel) = {
        this(connection, identifier, AbstractSharedCacheManager.get(identifier)(relay.traffic), communicator)
    }

    private val stateInstance = cache.get(3, SharedInstance[ConnectionState])

    override def getConnectionState: ConnectionState = stateInstance.get

    stateInstance.addListener(newState => {
        //val event = NetworkEvents.entityStateChange(this, newState, getConnectionState)
        //relay.eventNotifier.notifyEvent(relay.networkHooks, event)
    })

}
