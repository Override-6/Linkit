package fr.`override`.linkit.client.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.cache.SharedInstance
import fr.`override`.linkit.api.network.{AbstractRemoteEntity, ConnectionState}
import fr.`override`.linkit.api.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.system.event.network.NetworkEvents

class RelayNetworkEntity(relay: Relay, identifier: String, communicator: CommunicationPacketChannel)
    extends AbstractRemoteEntity(relay, identifier, communicator) {

    private val stateInstance = cache.get(3, SharedInstance[ConnectionState])

    override def getConnectionState: ConnectionState = stateInstance.get

    stateInstance.addListener(newState => {
        val event = NetworkEvents.entityStateChange(this, newState, getConnectionState)
        relay.eventNotifier.notifyEvent(event, relay.networkHooks)
    })

}
