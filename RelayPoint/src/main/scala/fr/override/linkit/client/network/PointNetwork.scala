package fr.`override`.linkit.client.network

import fr.`override`.linkit.api.connection.network.NetworkEntity
import fr.`override`.linkit.api.connection.network.cache.SharedCacheManager
import fr.`override`.linkit.api.connection.packet.traffic.PacketTraffic
import fr.`override`.linkit.client.ClientConnection
import fr.`override`.linkit.core.connection.network.cache.AbstractSharedCacheManager
import fr.`override`.linkit.core.connection.network.cache.collection.{BoundedCollection, CollectionModification}
import fr.`override`.linkit.core.connection.network.{AbstractNetwork, SelfNetworkEntity}
import fr.`override`.linkit.core.connection.packet.traffic.channel.CommunicationPacketChannel

import java.sql.Timestamp

class PointNetwork(connection: ClientConnection, globalCache: SharedCacheManager) extends AbstractNetwork(connection, globalCache) {
    private implicit val traffic: PacketTraffic = connection.traffic

    override val connectionEntity: SelfNetworkEntity = new SelfNetworkEntity(connection, AbstractSharedCacheManager.get(connection.identifier))

    override protected val entities: BoundedCollection.Immutable[NetworkEntity] = {
        sharedIdentifiers
                .addListener((_, _, _) => if (entities != null) () /*println("entities are now : " + entities)*/) //debug purposes
                .mapped(createEntity)
                .addListener(handleTraffic)
    }

    override def startUpDate: Timestamp = globalCache(2)

    override def createRelayEntity(identifier: String, communicator: CommunicationPacketChannel): NetworkEntity = {
        new ConnectionNetworkEntity(connection, identifier, communicator)
    }

    def update(): Unit = {
        globalCache.update()
        connectionEntity.update()
    }

    private def handleTraffic(mod: CollectionModification, index: Int, entityOpt: Option[NetworkEntity]): Unit = {
        //lazy val entity = entityOpt.get
        /*val event = mod match {
            case ADD => NetworkEvents.entityAdded(entity)
            case REMOVE => NetworkEvents.entityRemoved(entity)
            case _ => return
        }
        relay.eventNotifier.notifyEvent(relay.networkHooks, event)
         */
    }

}