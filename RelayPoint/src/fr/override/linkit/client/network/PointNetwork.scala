package fr.`override`.linkit.client.network

import java.sql.Timestamp

import fr.`override`.linkit.api.network.cache.collection.BoundedCollection
import fr.`override`.linkit.api.network.{AbstractNetwork, NetworkEntity}
import fr.`override`.linkit.api.packet.traffic.dedicated.CommunicationPacketChannel
import fr.`override`.linkit.client.RelayPoint

class PointNetwork(relay: RelayPoint) extends AbstractNetwork(relay) {

    override protected val entities: BoundedCollection.Immutable[NetworkEntity] = {
        sharedIdentifiers
                .addListener((_, _, _) => if (entities != null) () /*println("entities are now : " + entities)*/) //debug purposes
                .mapped(createEntity)
    }
    //Once all entities are initialized for this relay, add himself to the network
    sharedIdentifiers
            .add(relay.identifier)
            .flush()

    override val startUpDate: Timestamp = globalCache(2)

    override def createRelayEntity(identifier: String, communicator: CommunicationPacketChannel): NetworkEntity = {
        new RelayNetworkEntity(relay, identifier, communicator)
    }
}
