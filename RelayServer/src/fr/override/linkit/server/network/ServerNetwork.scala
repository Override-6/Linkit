package fr.`override`.linkit.server.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.{AbstractNetwork, ConnectionState, NetworkEntity, SelfNetworkEntity}
import fr.`override`.linkit.api.packet.channel.{AsyncPacketChannel, SyncPacketChannel}
import fr.`override`.linkit.api.packet.collector.{AsyncPacketCollector, PacketCollector, SyncPacketCollector}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.Tuple3Packet
import fr.`override`.linkit.api.utils.Tuple3Packet._
import fr.`override`.linkit.server.RelayServer

class ServerNetwork(server: RelayServer) extends AbstractNetwork(server) {

    private var asyncCollector: PacketCollector.Async = _

    private val syncCollector = server.createCollector(2, SyncPacketCollector)

    override def addEntity(entity: NetworkEntity): Unit = {
        super.addEntity(entity)

        refreshNetwork(entity)
        entity.addOnStateUpdate(state => if (state == ConnectionState.CONNECTED) refreshNetwork(entity))
    }

    override protected def sendPacket(packet: Packet, coords: PacketCoordinates): Unit = {
        syncCollector.sendPacket(packet, coords.targetID)
    }

    override protected def updateEntityState(entity: NetworkEntity, state: ConnectionState): Unit = {
        //do not explicitly update the entity state because they already have connection state of their ClientConnection attributed object.
        val identifier = entity.identifier
        val packet = ("update", identifier, state.name())
        val entities = listEntities.filterNot(e => e.isInstanceOf[SelfNetworkEntity] || e.identifier == identifier)
        entities.foreach(e => asyncCollector.sendPacket(packet, e.identifier))
    }

    override def addEntity(identifier: String): Unit = super.addEntity(identifier)

    private def refreshNetwork(entity: NetworkEntity): Unit = {
        // notifies every connected entities that a new entity has been added to the network
        val identifier = entity.identifier
        val packet = ("add", identifier)
        val entities = listEntities.filterNot(e => e.isInstanceOf[SelfNetworkEntity] || e.identifier == identifier)
        entities.foreach(e => asyncCollector.sendPacket(packet, e.identifier))

        //adding all currently connected entities of this network into the new entity
        entities.foreach(e => asyncCollector.sendPacket(("add", e.identifier), identifier))
    }

    override protected def createEntity(identifier: String): NetworkEntity = {
        val asyncChannel = asyncCollector.subChannel(identifier, AsyncPacketChannel)
        val syncChannel = syncCollector.subChannel(identifier, SyncPacketChannel)
        val entity = new ConnectionNetworkEntity(asyncChannel, syncChannel, server.getConnection(identifier))

        entity.addOnStateUpdate(state => {
            if (state == ConnectionState.CLOSED) {
                removeEntity(identifier)
                asyncChannel.close()
                syncChannel.close()
            } else {
                /*
                *  If the entity is removed from the network,
                *  any state update packet does not have to be updated;
                *  it could cause problems with some implementations
                */
                updateEntityState(entity, state)
            }
        })
        entity
    }

    override def removeEntity(identifier: String): Unit = {
        getEntity(identifier).foreach(updateEntityState(_, ConnectionState.CLOSED))
        super.removeEntity(identifier)
    }

    override protected def getAsyncChannel: PacketCollector.Async = {
        if (asyncCollector == null)
            asyncCollector = server.createCollector(1, AsyncPacketCollector)
        asyncCollector
    }

    override protected def handleOrder(packet: Tuple3Packet, coords: PacketCoordinates): Boolean = {
        packet._1 match {
            case "versions" =>
                val target = packet._2
                val responseCoords = coords.reversed()
                if (target == server.identifier)
                    sendPacket((Relay.ApiVersion.toString, server.relayVersion.toString), responseCoords)
                else {
                    val entityOpt = getEntity(target)
                    if (entityOpt.isDefined) {
                        val entity = entityOpt.get
                        val api = entity.getApiVersion.toString
                        val relay = entity.getRelayVersion.toString
                        sendPacket((api, relay), responseCoords)
                        return true
                    }
                    sendPacket(("error", "error"), responseCoords)
                }
                true
            case _ => false
        }
    }


}
