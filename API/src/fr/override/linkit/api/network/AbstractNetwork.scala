package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.cache.collection.{BoundedCollection, CollectionModification}
import fr.`override`.linkit.api.network.cache.{ObjectPacket, SharedCacheHandler}
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.collector.CommunicationPacketCollector

abstract class AbstractNetwork(relay: Relay) extends Network {

    override val globalCache: SharedCacheHandler = SharedCacheHandler.global(relay.traffic)

    override val selfEntity: SelfNetworkEntity = new SelfNetworkEntity(relay)

    protected val entities: BoundedCollection.Immutable[NetworkEntity]
    private val communicator = relay.openCollector(9, CommunicationPacketCollector)

    override def listEntities: List[NetworkEntity] = entities.to(List)

    override def addOnEntityAdded(action: NetworkEntity => Unit): Unit = {
        entities.addListener((modKind, _, entity) => {
            if (modKind == CollectionModification.ADD && entity.isDefined)
                action(entity.orNull)
        })
    }

    override def getEntity(identifier: String): Option[NetworkEntity] = {
        if (entities != null)
            entities.find(_.identifier == identifier)
        else None
    }

    def createEntity(identifier: String): NetworkEntity = {
        if (getEntity(identifier).isDefined) {
            throw new IllegalArgumentException("This entity is already registered to the network !")
        }

        if (identifier == relay.identifier) {
            return selfEntity
        }
        createRelayEntity(identifier, communicator.subChannel(identifier, CommunicationPacketChannel))
    }

    def createRelayEntity(identifier: String, communicationChannel: CommunicationPacketChannel): NetworkEntity

    communicator.addRequestListener((packet, coords) => {
        val sender = coords.senderID
        packet match {
            case ObjectPacket(("getProp", name: String)) =>
                val prop = relay.properties.get(name).orNull
                communicator.sendResponse(ObjectPacket(prop), sender)

            case ObjectPacket(("setProp", name: String, value)) =>
                relay.properties.putProperty(name, value)

            case ObjectPacket("vAPI") =>
                communicator.sendResponse(ObjectPacket(Relay.ApiVersion), sender)
            case ObjectPacket("vImpl") =>
                communicator.sendResponse(ObjectPacket(relay.relayVersion), sender)
        }
    })

}

