package fr.`override`.linkit.client.network

import java.sql.Timestamp

import fr.`override`.linkit.api.connection.network.NetworkEntity
import fr.`override`.linkit.api.connection.packet.traffic.PacketTraffic
import fr.`override`.linkit.client.ClientConnection
import fr.`override`.linkit.core.connection.network.cache.AbstractSharedCacheManager
import fr.`override`.linkit.core.connection.network.cache.collection.{BoundedCollection, CollectionModification}
import fr.`override`.linkit.core.connection.network.{AbstractNetwork, SelfNetworkEntity}
import fr.`override`.linkit.core.connection.packet.traffic.channel.CommunicationPacketChannel

class PointNetwork(connection: ClientConnection, globalCache: AbstractSharedCacheManager) extends AbstractNetwork(connection, globalCache) {
    private implicit val traffic: PacketTraffic = connection.traffic

    override val selfEntity: SelfNetworkEntity = new SelfNetworkEntity(relay, AbstractSharedCacheManager.get(relay.identifier))

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
        lazy val entity = entityOpt.get
        val event = mod match {
            case ADD => NetworkEvents.entityAdded(entity)
            case REMOVE => NetworkEvents.entityRemoved(entity)
            case _ => return
        }
        relay.eventNotifier.notifyEvent(relay.networkHooks, event)
    }

}