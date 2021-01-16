package fr.`override`.linkit.client.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.{AbstractNetworkEntity, ConnectionState}
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.utils.cache.SharedCacheHandler

class RelayNetworkEntity(relay: Relay, cache: SharedCacheHandler, identifier: String, communicator: CommunicationPacketChannel)
        extends AbstractNetworkEntity(relay, cache, identifier, communicator) {
    //private val sharedState = SharedCaches.sharedInstance(7)

    override def addOnStateUpdate(action: ConnectionState => Unit): Unit = () //sharedState.addListener(action)

    override def getConnectionState: ConnectionState = ConnectionState.CLOSED
}
