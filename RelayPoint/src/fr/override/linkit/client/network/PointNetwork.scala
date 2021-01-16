package fr.`override`.linkit.client.network

import java.sql.Timestamp

import fr.`override`.linkit.api.network.{AbstractNetwork, NetworkEntity}
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.utils.cache.collection.{BoundedCollection, SharedCollection}
import fr.`override`.linkit.client.RelayPoint

class PointNetwork(relay: RelayPoint) extends AbstractNetwork(relay) {

    override protected val entities: BoundedCollection.Immutable[NetworkEntity] = {
        cache
                .open(3, SharedCollection[String])
                .addListener((_, _, _) => if (entities != null) println("entities are now : " + entities)) //debug purposes
                .add(relay.identifier)
                .flush()
                .mapped(createEntity)
    }
    println("Entities : " + entities)

    override val onlineTimeStamp: Timestamp = cache(2)

    override def createRelayEntity(identifier: String, communicator: CommunicationPacketChannel): NetworkEntity = {
        new RelayNetworkEntity(relay, cache, identifier, communicator)
    }
}
