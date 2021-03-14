package fr.`override`.linkit.server.network

import fr.`override`.linkit.api.network.cache.{SharedCacheHandler, SharedInstance}
import fr.`override`.linkit.api.network.{AbstractRemoteEntity, ConnectionState}
import fr.`override`.linkit.api.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.server.RelayServer

class ConnectionNetworkEntity private(server: RelayServer,
                                      identifier: String,
                                      cacheHandler: SharedCacheHandler,
                                      communicator: CommunicationPacketChannel)
        extends AbstractRemoteEntity(server, identifier, cacheHandler, communicator) {

    def this(server: RelayServer, identifier: String, communicator: CommunicationPacketChannel) = {
        this(server, identifier, SharedCacheHandler.get(identifier, ServerSharedCacheHandler())(server.traffic), communicator)
    }

    private val connection = server.getConnection(identifier).get
    private val sharedState = cache.get(3, SharedInstance[ConnectionState])
            .set(ConnectionState.CONNECTED) //technically already connected

    override def getConnectionState: ConnectionState = connection.getState
    connection.addConnectionStateListener(state => server.runLater(sharedState.set(state)))

}
