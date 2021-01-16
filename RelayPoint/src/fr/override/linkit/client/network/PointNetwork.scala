package fr.`override`.linkit.client.network

import java.sql.Timestamp

import fr.`override`.linkit.api.network.{AbstractNetwork, NetworkEntity}
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.client.RelayPoint

class PointNetwork(relay: RelayPoint) extends AbstractNetwork(relay) {

    override protected val entities: BoundedCollection.Immutable[NetworkEntity] = {
        SharedCollection
                .open[String](3)
                .addListener((a, b, c) => println(a, b, c))
                .awaitInitialised()
                .add(relay.identifier)
                .flush()
                .mapped(createEntity)
    }

    override val onlineTimeStamp: Timestamp = SharedCaches.retrieveInstance(55)

    override def createRelayEntity(identifier: String, communicator: CommunicationPacketChannel): NetworkEntity = {
        new RelayNetworkEntity(relay, identifier, communicator)
    }
}
