package fr.`override`.linkit.client.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.{AbstractNetwork, ConnectionState, NetworkEntity}
import fr.`override`.linkit.api.packet.channel.{AsyncPacketChannel, PacketChannel, SyncPacketChannel}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.client.RelayPoint

class PointNetwork(relay: RelayPoint) extends AbstractNetwork(relay) {

    private var asyncChannel: PacketChannel.Async = _
    private val syncChannel = relay.createChannel(2, Relay.ServerIdentifier, SyncPacketChannel)

    override protected def createEntity(identifier: String): NetworkEntity = {
        new RelayNetworkEntity(relay, getAsyncChannel, syncChannel, identifier)
    }

    override protected def updateEntityState(entity: NetworkEntity, state: ConnectionState): Unit = {
        val relayEntity = entity.asInstanceOf[RelayNetworkEntity]
        relayEntity.onConnectionChange(state)
        println(s"state updated for ${entity.identifier} = ${state}")
        if (state == ConnectionState.CLOSED)
            removeEntity(entity.identifier)
    }

    override protected def sendPacket(packet: Packet, coords: PacketCoordinates): Unit = {
        syncChannel.sendPacket(packet)
    }

    override protected def getAsyncChannel: PacketChannel.Async = {
        if (asyncChannel == null)
            asyncChannel = relay.createChannel(1, Relay.ServerIdentifier, AsyncPacketChannel)
        asyncChannel
    }

    def init(): Unit = {
        addEntity(createEntity(Relay.ServerIdentifier))
    }

}
