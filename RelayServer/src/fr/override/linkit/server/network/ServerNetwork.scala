package fr.`override`.linkit.server.network

import java.sql.Timestamp

import fr.`override`.linkit.api.network.{AbstractNetwork, NetworkEntity}
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.utils.cache.collection.SharedCollection
import fr.`override`.linkit.server.RelayServer

class ServerNetwork(server: RelayServer)(implicit traffic: PacketTraffic) extends AbstractNetwork(server) {

    override val onlineTimeStamp: Timestamp = new Timestamp(System.currentTimeMillis())
    override protected val entities: BoundedCollection.Immutable[NetworkEntity] = {
        SharedCollection
                .open[String](3)
                .addListener((a, b, c) => println(a, b, c)) //debug purposes
                .add(server.identifier)
                .flush()
                .mapped(createEntity)
    }

    override def createRelayEntity(identifier: String, communicator: CommunicationPacketChannel): NetworkEntity = {
        new ConnectionNetworkEntity(server, identifier, communicator)
    }
}
