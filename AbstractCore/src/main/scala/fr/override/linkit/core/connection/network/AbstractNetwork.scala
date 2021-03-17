package fr.`override`.linkit.core.connection.network

import fr.`override`.linkit.core.connection
import fr.`override`.linkit.core.connection.network.cache.collection
import fr.`override`.linkit.core.connection.packet
import fr.`override`.linkit.core.connection.packet.traffic
import fr.`override`.linkit.core.connection.packet.traffic.channel
import fr.`override`.linkit.skull.Relay
import fr.`override`.linkit.skull.connection.network.cache.SharedCacheHandler
import fr.`override`.linkit.skull.connection.network.cache.collection.{BoundedCollection, SharedCollection}
import fr.`override`.linkit.skull.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.`override`.linkit.skull.connection.packet.traffic.ChannelScope
import fr.`override`.linkit.skull.connection.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.skull.internal.system.event.network.NetworkEvents

abstract class AbstractNetwork(relay: Relay, override val globalCache: cache.SharedCacheHandler) extends Network {

    relay.packetTranslator.completeInitialisation(globalCache)

    protected val sharedIdentifiers: cache.collection.SharedCollection[String] = globalCache
            .get(3, cache.collection.SharedCollection.set[String])

    protected val entities: collection.BoundedCollection.Immutable[NetworkEntity]
    protected val communicator: packet.traffic.channel.CommunicationPacketChannel = relay.getInjectable(9, ChannelScope.broadcast, channel.CommunicationPacketChannel.providable)
    private val notifier = relay.eventNotifier

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
                import relay.networkHooks
                notifier.notifyEvent(event)
        }
    })

}

