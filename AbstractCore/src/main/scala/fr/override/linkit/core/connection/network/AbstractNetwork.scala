package fr.`override`.linkit.core.connection.network

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.connection.network.{Network, NetworkEntity}
import fr.`override`.linkit.api.connection.packet.traffic.ChannelScope
import fr.`override`.linkit.core.connection.network.cache.AbstractSharedCacheManager
import fr.`override`.linkit.core.connection.network.cache.collection.BoundedCollection
import fr.`override`.linkit.core.connection.packet.traffic.channel.CommunicationPacketChannel


abstract class AbstractNetwork(connection: ConnectionContext, override val globalCache: AbstractSharedCacheManager) extends Network {

    protected val sharedIdentifiers: cache.collection.SharedCollection[String] = globalCache
            .get(3, cache.collection.SharedCollection.set[String])

    protected val entities: BoundedCollection.Immutable[NetworkEntity]
    protected val communicator: CommunicationPacketChannel = connection.getInjectable(9, ChannelScope.broadcast, channel.CommunicationPacketChannel.providable)
    private val notifier = connection.eventNotifier

    override def listEntities: List[NetworkEntity] = entities.to(List)

    override def getEntity(identifier: String): Option[NetworkEntity] = {
        if (entities != null)
            entities.find(_.identifier == identifier)
        else None
    }

    override def isConnected(identifier: String): Boolean = getEntity(identifier).isDefined

    protected def createEntity(identifier: String): NetworkEntity = {
        if (identifier == relay.identifier) {
            return selfEntity
        }

        println(s"Creating entity $identifier")
        val channel = communicator.subInjectable(Array(identifier), traffic.channel.CommunicationPacketChannel.providable, true)
        val ent = createRelayEntity(identifier, channel)
        ent
    }

    def createRelayEntity(identifier: String, communicationChannel: connection.packet.traffic.channel.CommunicationPacketChannel): NetworkEntity

    communicator.addRequestListener((packet, coords) => {
        val sender = coords.senderID
        packet match {
            case ObjectPacket(("getProp", name: String)) =>
                val prop = relay.properties.get(name).orNull
                communicator.sendResponse(ObjectPacket(prop), sender)

            case ObjectPacket(("setProp", name: String, newValue: Serializable)) =>
                val oldValue = relay.properties.putProperty(name, newValue)
                val entity = getEntity(sender).get
                val event = NetworkEvents.remotelyCurrentPropertyChange(entity, name, newValue, oldValue)
                notifier.notifyEvent(event)
        }
    })

}

