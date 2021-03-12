package fr.`override`.linkit.server.network

import fr.`override`.linkit.api.network.cache.SharedInstance
import fr.`override`.linkit.api.network.cache.collection.{BoundedCollection, CollectionModification}
import fr.`override`.linkit.api.network.{AbstractNetwork, ConnectionState, NetworkEntity}
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.system.evente.network.NetworkEvents
import fr.`override`.linkit.server.RelayServer

import java.sql.Timestamp

class ServerNetwork(server: RelayServer)(implicit traffic: PacketTraffic) extends AbstractNetwork(server) {

    override val startUpDate: Timestamp = globalCache.post(2, new Timestamp(System.currentTimeMillis()))

    override protected val entities: BoundedCollection.Immutable[NetworkEntity] = {
        sharedIdentifiers
                .addListener((_, _, _) => if (entities != null) () /*println("entities are now : " + entities)*/) //debug purposes
                .add(server.identifier)
                .flush()
                .mapped(createEntity)
                .addListener(handleTraffic)
    }
    selfEntity
            .cache
            .get(3, SharedInstance[ConnectionState])
            .set(ConnectionState.CONNECTED) //technically already connected

    override def createRelayEntity(identifier: String, communicator: CommunicationPacketChannel): NetworkEntity = {
        new ConnectionNetworkEntity(server, identifier, communicator)
    }

    def removeEntity(identifier: String): Unit = {
        getEntity(identifier)
                .foreach(entity => {
                    if (entity.getConnectionState != ConnectionState.CLOSED)
                        throw new IllegalStateException(s"Could not remove entity '$identifier' from network as long as it still open")
                    sharedIdentifiers.remove(identifier)
                })
    }

    private def handleTraffic(mod: CollectionModification, index: Int, entityOpt: Option[NetworkEntity]): Unit = {
        import CollectionModification._
        lazy val entity = entityOpt.get
        val event = mod match {
            case ADD => NetworkEvents.entityAdded(entity)
            case REMOVE => NetworkEvents.entityRemoved(entity)
            case _ => return
        }
        server.eventNotifier.notifyEvent(server.networkHooks, event)
    }
}
