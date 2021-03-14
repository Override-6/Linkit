package fr.`override`.linkit.client.network

import fr.`override`.linkit.api.network.cache.SharedCacheHandler
import fr.`override`.linkit.api.network.cache.collection.{BoundedCollection, CollectionModification}
import fr.`override`.linkit.api.network.{AbstractNetwork, NetworkEntity, SelfNetworkEntity}
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.system.evente.network.NetworkEvents
import fr.`override`.linkit.client.RelayPoint

import java.sql.Timestamp

class PointNetwork private(relay: RelayPoint, globalCache: SharedCacheHandler) extends AbstractNetwork(relay, globalCache) {
    private implicit val traffic: PacketTraffic = relay.traffic

    def this(relay: RelayPoint) = {
        this(relay, SharedCacheHandler.get("Global Shared Cache")(relay.traffic))
    }

    override val selfEntity: SelfNetworkEntity = new SelfNetworkEntity(relay, SharedCacheHandler.get(relay.identifier))

    override protected val entities: BoundedCollection.Immutable[NetworkEntity] = {
        sharedIdentifiers
                .addListener((_, _, _) => if (entities != null) () /*println("entities are now : " + entities)*/) //debug purposes
                .mapped(createEntity)
                .addListener(handleTraffic)
    }

    override def startUpDate: Timestamp = globalCache(2)

    override def createRelayEntity(identifier: String, communicator: CommunicationPacketChannel): NetworkEntity = {
        new RelayNetworkEntity(relay, identifier, communicator)
    }

    def update(): Unit = {
        globalCache.update()
        selfEntity.update()
    }

    private def handleTraffic(mod: CollectionModification, index: Int, entityOpt: Option[NetworkEntity]): Unit = {
        import CollectionModification._
        lazy val entity = entityOpt.get
        val event = mod match {
            case ADD => NetworkEvents.entityAdded(entity)
            case REMOVE => NetworkEvents.entityRemoved(entity)
            case _ => return
        }
        relay.eventNotifier.notifyEvent(relay.networkHooks, event)
    }

}