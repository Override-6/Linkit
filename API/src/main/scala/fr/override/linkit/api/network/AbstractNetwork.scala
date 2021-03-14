package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.cache.SharedCacheHandler
import fr.`override`.linkit.api.network.cache.collection.{BoundedCollection, SharedCollection}
import fr.`override`.linkit.api.packet.fundamental.RefPacket.ObjectPacket
import fr.`override`.linkit.api.packet.traffic.ChannelScope
import fr.`override`.linkit.api.packet.traffic.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.system.evente.network.NetworkEvents

abstract class AbstractNetwork(relay: Relay, override val globalCache: SharedCacheHandler) extends Network {

    relay.packetTranslator.completeInitialisation(globalCache)

    protected val sharedIdentifiers: SharedCollection[String] = globalCache
            .get(3, SharedCollection.set[String])

    protected val entities: BoundedCollection.Immutable[NetworkEntity]
    protected val communicator: CommunicationPacketChannel = relay.getInjectable(9, ChannelScope.broadcast, CommunicationPacketChannel.providable)
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
        val channel = communicator.subInjectable(Array(identifier), CommunicationPacketChannel.providable, true)
        val ent = createRelayEntity(identifier, channel)
        ent
    }

    def createRelayEntity(identifier: String, communicationChannel: CommunicationPacketChannel): NetworkEntity

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

