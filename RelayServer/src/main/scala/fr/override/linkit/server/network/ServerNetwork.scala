package fr.`override`.linkit.server.network

import fr.`override`.linkit.api.connection.network.cache.SharedCacheManager
import fr.`override`.linkit.api.connection.packet.traffic.PacketTraffic
import fr.`override`.linkit.core.connection.network.cache.AbstractSharedCacheManager
import fr.`override`.linkit.core.connection.network.cache.collection.BoundedCollection
import fr.`override`.linkit.core.connection.network.{AbstractNetwork, SelfNetworkEntity}
import fr.`override`.linkit.server.ServerApplicationContext
import fr.`override`.linkit.server.connection.ServerExternalConnection

import java.sql.Timestamp

class ServerNetwork private(serverContext: ServerApplicationContext, globalCache: SharedCacheManager)(implicit traffic: PacketTraffic) extends AbstractNetwork(serverContext, serverContext.identifier, globalCache) {

    def this(server: ServerApplicationContext, traffic: PacketTraffic) = {
        this(server, AbstractSharedCacheManager.get("Global Shared Cache", server.identifier, ServerSharedCacheManager())(traffic))(traffic)
    }

    override val selfEntity: SelfNetworkEntity =
        new SelfNetworkEntity(server, SharedCacheManager.get(server.identifier, ServerSharedCacheManager()))

    override val startUpDate: Timestamp = globalCache.post(2, new Timestamp(System.currentTimeMillis()))

    override protected val entities: BoundedCollection.Immutable[NetworkEntity] = {
        sharedIdentifiers
                .addListener((_, _, _) => if (entities != null) println("entities are now : " + entities)) //debug purposes
                .add(server.identifier)
                .flush()
                .mapped(createEntity)
                .addListener(handleTraffic)
    }
    selfEntity
            .cache
            .get(3, SharedInstance[ConnectionState])
            .set(ConnectionState.CONNECTED) //technically always connected

    override protected def createEntity(identifier: String): NetworkEntity = {
        //TODO Create remote breakpoints
        //FIXME Could return an unexpected if another packet (ex: setProperty) is received into this communicator.
        super.createEntity(identifier)
    }

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

    private[server] def addEntity(connection: ServerExternalConnection): Unit = {
        sharedIdentifiers.add(connection.identifier)
    }

    private def handleTraffic(mod: CollectionModification, index: Int, entityOpt: Option[NetworkEntity]): Unit = {
        lazy val entity = entityOpt.orNull //get
        println(s"mod = ${mod}")
        println(s"index = ${index}")
        println(s"entity = ${entity}")
        val event = mod match {
            case ADD => NetworkEvents.entityAdded(entity)
            case REMOVE => NetworkEvents.entityRemoved(entity)
            case _ => return
        }
        server.eventNotifier.notifyEvent(server.networkHooks, event)
    }

}
