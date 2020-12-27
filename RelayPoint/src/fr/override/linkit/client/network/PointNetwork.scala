package fr.`override`.linkit.client.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.packet.channel.PacketChannel
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.network.{AbstractNetwork, ConnectionState, NetworkEntity}
import fr.`override`.linkit.api.utils.AsyncExecutionContext.context
import fr.`override`.linkit.client.RelayPoint

import scala.concurrent.Future
import scala.util.control.NonFatal

class PointNetwork(relay: RelayPoint) extends AbstractNetwork(relay) {

    private var asyncChannel: PacketChannel.Async = _
    private val syncChannel = relay.createSyncChannel(Relay.ServerIdentifier, 2)

    override protected def createEntity(identifier: String): NetworkEntity = {
        new RelayNetworkEntity(relay, getAsyncChannel, syncChannel, identifier)
    }

    override protected def updateEntityState(entity: NetworkEntity, state: ConnectionState): Unit = {
        val relayEntity = entity.asInstanceOf[RelayNetworkEntity]
        relayEntity.onConnectionChange(state)
        if (state == ConnectionState.DISCONNECTED)
            removeEntity(entity.identifier)
    }

    override protected def sendPacket(packet: Packet, coords: PacketCoordinates): Unit = {
        syncChannel.sendPacket(packet)
    }

    override protected def getAsyncChannel: PacketChannel.Async = {
        if (asyncChannel == null)
            asyncChannel = relay.createAsyncChannel(Relay.ServerIdentifier, 1)
        asyncChannel
    }

    //immediately registers the server once connected
    relay.addConnectionListener(state => {
        Future {
            if (state == ConnectionState.CONNECTED && getEntity(Relay.ServerIdentifier).isEmpty) try {
                addEntity(new RelayNetworkEntity(relay, asyncChannel, syncChannel, Relay.ServerIdentifier))
            } catch {
                case NonFatal(e) => e.printStackTrace()
            }
        }(context)
    })

}
