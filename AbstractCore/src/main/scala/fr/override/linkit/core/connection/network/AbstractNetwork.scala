package fr.`override`.linkit.core.connection.network

import fr.`override`.linkit.api.connection.network.cache.SharedCacheManager
import fr.`override`.linkit.api.connection.network.{Network, NetworkEntity}
import fr.`override`.linkit.api.connection.packet.traffic.{ChannelScope, PacketInjectableContainer}
import fr.`override`.linkit.core.connection.network.cache.collection.BoundedCollection
import fr.`override`.linkit.core.connection.packet.traffic.channel.CommunicationPacketChannel


abstract class AbstractNetwork(container: PacketInjectableContainer,
                               selfIdentifier: String,
                               override val globalCache: SharedCacheManager) extends Network {

    protected val sharedIdentifiers: cache.collection.SharedCollection[String] = globalCache
            .get(3, cache.collection.SharedCollection.set[String])

    protected val entities: BoundedCollection.Immutable[NetworkEntity]
    protected val communicator: CommunicationPacketChannel =
        container.getInjectable(9, ChannelScope.broadcast, CommunicationPacketChannel.providable)

    override def listEntities: List[NetworkEntity] = entities.to(List)

    override def getEntity(identifier: String): Option[NetworkEntity] = {
        if (entities != null)
            entities.find(_.identifier == identifier)
        else None
    }

    override def isConnected(identifier: String): Boolean = getEntity(identifier).isDefined

    protected def createEntity(identifier: String): NetworkEntity = {
        if (identifier == selfIdentifier) {
            return connectionEntity
        }

        println(s"Creating entity $identifier")
        val channel = communicator.subInjectable(Array(identifier), CommunicationPacketChannel.providable, true)
        val ent = createRelayEntity(identifier, channel)
        ent
    }

    def createRelayEntity(identifier: String, communicationChannel: CommunicationPacketChannel): NetworkEntity

}

