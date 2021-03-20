package fr.`override`.linkit.server.network

import fr.`override`.linkit.server.RelayServer

class ConnectionNetworkEntity private(server: RelayServer,
                                      identifier: String,
                                      cacheHandler: SharedCacheManager,
                                      communicator: CommunicationPacketChannel)
        extends AbstractRemoteEntity(server, identifier, cacheHandler, communicator) {

    def this(server: RelayServer, identifier: String, communicator: CommunicationPacketChannel) = {
        this(server, identifier, SharedCacheManager.get(identifier, ServerSharedCacheManager())(server.traffic), communicator)
    }

    private val connection = server.getConnection(identifier).get
    private val sharedState = cache.get(3, SharedInstance[ConnectionState])
            .set(ConnectionState.CONNECTED) //technically already connected

    override def getConnectionState: ConnectionState = connection.getState
    connection.addConnectionStateListener(state => server.runLater(sharedState.set(state)))

}
