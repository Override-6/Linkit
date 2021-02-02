package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.cache.collection.{BoundedCollection, CollectionModification, SharedCollection}
import fr.`override`.linkit.api.network.cache.{ObjectPacket, SharedCacheHandler}
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.collector.CommunicationPacketCollector

abstract class AbstractNetwork(relay: Relay) extends Network {

    override val globalCache: SharedCacheHandler = SharedCacheHandler.create("Global Shared Cache", Relay.ServerIdentifier)(relay.traffic)

    override val selfEntity: SelfNetworkEntity = new SelfNetworkEntity(relay)


    protected val sharedIdentifiers: SharedCollection[String] = globalCache
            .open(3, SharedCollection.set[String])

    protected val entities: BoundedCollection.Immutable[NetworkEntity]
    private val communicator = relay.openCollector(9, CommunicationPacketCollector.providable)

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

    //Will replace the entity if the identifier is already present in the network's entities cache.
    def createEntity(identifier: String): NetworkEntity = {
        if (identifier == relay.identifier) {
            return selfEntity
        }

        val channel = communicator.subChannel(identifier, CommunicationPacketChannel.providable, true)
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

            case ObjectPacket(("setProp", name: String, value)) =>
                relay.properties.putProperty(name, value)
        }
    })

}

