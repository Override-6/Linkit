package fr.`override`.linkit.client.network

import fr.`override`.linkit.api.network.cache.collection.BoundedCollection
import fr.`override`.linkit.api.network.{AbstractNetwork, NetworkEntity}
import fr.`override`.linkit.api.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.client.RelayPoint

import java.sql.Timestamp

class PointNetwork(relay: RelayPoint) extends AbstractNetwork(relay) {

    override protected val entities: BoundedCollection.Immutable[NetworkEntity] = {
        sharedIdentifiers
            .addListener((_, _, _) => if (entities != null) () /*println("entities are now : " + entities)*/) //debug purposes
            .mapped(createEntity)
    }
    override val startUpDate: Timestamp = globalCache(2)

    //Once all entities are initialized for this relay, add himself to the network
    sharedIdentifiers
        .flush()
        .add(relay.identifier)

    override def createRelayEntity(identifier: String, communicator: CommunicationPacketChannel): NetworkEntity = {
        new RelayNetworkEntity(relay, identifier, communicator)
    }
}
