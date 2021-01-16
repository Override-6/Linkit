package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.collector.CommunicationPacketCollector
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.utils.cache.ObjectPacket
import fr.`override`.linkit.api.utils.cache.collection.{BoundedCollection, CollectionModification, SharedCollection}

abstract class AbstractNetwork(relay: Relay) extends Network {



    protected implicit val traffic: PacketTraffic = relay.traffic
    protected val entities: BoundedCollection.Immutable[NetworkEntity]
    private val communicator = relay.openCollector(9, CommunicationPacketCollector)
    private val sharedFragments = SharedCollection.open[String](6)(relay.traffic)

    private val fragmentHandler = relay.extensionLoader.fragmentHandler
    sharedFragments.set(fragmentHandler.listRemoteFragments().map(_.nameIdentifier).toArray)
    fragmentHandler.addOnRemoteFragmentsAdded(sharedFragments.add(_))

    println(s"sharedFragments = ${sharedFragments}")
    sharedFragments.addListener((a, b, c) => println("Fragments modified : " + a, b, c))

    override def listEntities: List[NetworkEntity] = entities.to(List)

    override def getEntity(identifier: String): Option[NetworkEntity] = entities.find(_.identifier == identifier)

    override def addOnEntityAdded(action: NetworkEntity => Unit): Unit = {
        entities.addListener((modKind, _, entity) => {
            if (modKind == CollectionModification.ADD)
                action(entity)
        })
    }

    def createEntity(identifier: String): NetworkEntity = {
        if (identifier == relay.identifier)
            return new SelfNetworkEntity(relay)
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

