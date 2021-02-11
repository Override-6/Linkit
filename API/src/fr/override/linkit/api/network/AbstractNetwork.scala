package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.cache.SharedCacheHandler
import fr.`override`.linkit.api.network.cache.collection.{BoundedCollection, CollectionModification, SharedCollection}
import fr.`override`.linkit.api.packet.fundamental.ValPacket
import fr.`override`.linkit.api.packet.traffic.ChannelScope
import fr.`override`.linkit.api.packet.traffic.channel.CommunicationPacketChannel

abstract class AbstractNetwork(relay: Relay) extends Network {

    override val globalCache: SharedCacheHandler = SharedCacheHandler.create("Global Shared Cache", Relay.ServerIdentifier)(relay.traffic)
    relay.packetTranslator.completeInitialisation(globalCache)

    override val selfEntity: SelfNetworkEntity = new SelfNetworkEntity(relay)


    protected val sharedIdentifiers: SharedCollection[String] = globalCache
            .get(3, SharedCollection.set[String])

    protected val entities: BoundedCollection.Immutable[NetworkEntity]
    private val communicator = relay.createInjectable(9, ChannelScope.broadcast, CommunicationPacketChannel.providable)

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

        val channel = communicator.subInjectable(Array(identifier), CommunicationPacketChannel.providable, true)
        val ent = createRelayEntity(identifier, channel)
        ent
    }

    def createRelayEntity(identifier: String, communicationChannel: CommunicationPacketChannel): NetworkEntity

    communicator.addRequestListener((packet, coords) => {
        val sender = coords.senderID
        packet match {
            case ValPacket(("getProp", name: String)) =>
                val prop = relay.properties.get(name).orNull
                communicator.sendResponse(ValPacket(prop), sender)

            case ValPacket(("setProp", name: String, value)) =>
                relay.properties.putProperty(name, value)
        }
    })

}

