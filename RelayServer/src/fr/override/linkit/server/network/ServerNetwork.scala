package fr.`override`.linkit.server.network

import java.sql.Timestamp

import fr.`override`.linkit.api.network.cache.collection.{BoundedCollection, SharedCollection}
import fr.`override`.linkit.api.network.{AbstractNetwork, NetworkEntity}
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.server.RelayServer

class ServerNetwork(server: RelayServer)(implicit traffic: PacketTraffic) extends AbstractNetwork(server) {

    override val onlineTimeStamp: Timestamp = new Timestamp(System.currentTimeMillis())
    globalCache.post(2, onlineTimeStamp)

    override protected val entities: BoundedCollection.Immutable[NetworkEntity] = {
        globalCache
                .open(3, SharedCollection[String])
                .addListener((_, _, _) => if (entities != null) println("entities are now : " + entities)) //debug purposes
                .add(server.identifier)
                .flush()
                .mapped(createEntity)
    }
    println("ENTITIES : " + entities)

    override def createRelayEntity(identifier: String, communicator: CommunicationPacketChannel): NetworkEntity = {
        new ConnectionNetworkEntity(server, identifier, communicator)
    }
}
