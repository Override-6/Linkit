package fr.`override`.linkit.client.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.cache.SharedInstance
import fr.`override`.linkit.api.network.{AbstractRemoteEntity, ConnectionState}
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel

class RelayNetworkEntity(relay: Relay, identifier: String, communicator: CommunicationPacketChannel)
        extends AbstractRemoteEntity(relay, identifier, communicator) {

    private val stateInstance = cache.open(3, SharedInstance[ConnectionState])

    override def addOnStateUpdate(action: ConnectionState => Unit): Unit = stateInstance.addListener(action)

    override def getConnectionState: ConnectionState = stateInstance.get
}
