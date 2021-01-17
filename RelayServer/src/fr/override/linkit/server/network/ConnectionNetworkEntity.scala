package fr.`override`.linkit.server.network

import fr.`override`.linkit.api.network.{AbstractRemoteEntity, ConnectionState}
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.server.RelayServer

class ConnectionNetworkEntity(server: RelayServer, identifier: String, communicator: CommunicationPacketChannel)
        extends AbstractRemoteEntity(server, identifier, communicator) {

    private val connection = server.getConnection(identifier)

    override def addOnStateUpdate(action: ConnectionState => Unit): Unit = connection.addConnectionStateListener(action)

    override def getConnectionState: ConnectionState = connection.getState

}
