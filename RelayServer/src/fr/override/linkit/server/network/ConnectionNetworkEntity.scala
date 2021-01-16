package fr.`override`.linkit.server.network

import fr.`override`.linkit.api.network.{AbstractNetworkEntity, ConnectionState}
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.utils.cache.SharedCacheHandler
import fr.`override`.linkit.server.RelayServer

class ConnectionNetworkEntity(server: RelayServer, cache: SharedCacheHandler, identifier: String, communicator: CommunicationPacketChannel)
        extends AbstractNetworkEntity(server, cache, identifier, communicator) {

    private val connection = server.getConnection(identifier)

    override def addOnStateUpdate(action: ConnectionState => Unit): Unit = connection.addConnectionStateListener(action)

    override def getConnectionState: ConnectionState = connection.getState

}
