package fr.`override`.linkit.client.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.{AbstractRemoteEntity, ConnectionState}
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel

class RelayNetworkEntity(relay: Relay, identifier: String, communicator: CommunicationPacketChannel)
        extends AbstractRemoteEntity(relay, identifier, communicator) {
    //private val sharedState = SharedCaches.sharedInstance(7)

    override def addOnStateUpdate(action: ConnectionState => Unit): Unit = () //sharedState.addListener(action)

    override def getConnectionState: ConnectionState = ConnectionState.CLOSED
}
