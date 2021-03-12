package fr.`override`.linkit.client.network

import java.sql.Timestamp

import fr.`override`.linkit.api.network.cache.collection.{BoundedCollection, CollectionModification}
import fr.`override`.linkit.api.network.{AbstractNetwork, NetworkEntity}
import fr.`override`.linkit.api.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.system.event.network.NetworkEvents
import fr.`override`.linkit.client.RelayPoint

class PointNetwork(relay: RelayPoint) extends AbstractNetwork(relay) {

    override protected val entities: BoundedCollection.Immutable[NetworkEntity] = {
        sharedIdentifiers
            .addListener((_, _, _) => if (entities != null) () /*println("entities are now : " + entities)*/) //debug purposes
            .mapped(createEntity)
            .addListener(handleTraffic)
    }
    override val startUpDate: Timestamp = globalCache(2)

    //Once all entities are initialized for this relay, add himself to the network
    sharedIdentifiers
        .flush()
        .add(relay.identifier)

    override def createRelayEntity(identifier: String, communicator: CommunicationPacketChannel): NetworkEntity = {
        new RelayNetworkEntity(relay, identifier, communicator)
    }

    private def handleTraffic(mod: CollectionModification, index: Int, entityOpt: Option[NetworkEntity]): Unit = {
        import CollectionModification._
        lazy val entity = entityOpt.get
        val event = mod match {
            case ADD => NetworkEvents.entityAdded(entity)
            case REMOVE => NetworkEvents.entityRemoved(entity)
            case _ => return
        }
        relay.eventNotifier.notifyEvent(event, relay.networkHooks)
    }
}