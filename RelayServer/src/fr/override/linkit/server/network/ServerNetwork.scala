package fr.`override`.linkit.server.network

import fr.`override`.linkit.api.packet.collector.PacketCollector
import fr.`override`.linkit.api.utils.Tuple3Packet._
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates}
import fr.`override`.linkit.api.system.network.{AbstractNetwork, ConnectionState, NetworkEntity, SelfNetworkEntity}
import fr.`override`.linkit.server.RelayServer

class ServerNetwork(relay: RelayServer) extends AbstractNetwork(relay) {
    private var asyncCollector: PacketCollector.Async = _
    private val syncCollector: PacketCollector.Sync = relay.createSyncCollector(2)

    override protected def createEntity(identifier: String): NetworkEntity = {
        val entity = new ConnectionNetworkEntity(asyncCollector, syncCollector, relay.getConnection(identifier))
        entity.addOnStateUpdate(state => {
            if (state == ConnectionState.DISCONNECTED)
                removeEntity(identifier)
            updateEntityState(entity, state)
        })
        entity
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

    override def addEntity(entity: NetworkEntity): Unit = {
        val identifier = entity.identifier
        super.addEntity(entity)

        // notifies every connected entities that a new entity has been added to the network
        val packet = ("add", identifier)
        val entities = listEntities.filterNot(e => e.isInstanceOf[SelfNetworkEntity] || e.identifier == identifier)
        entities.foreach(e => asyncCollector.sendPacket(packet, e.identifier))

        //adding all currently connected entities of this network to the new entity
        entities.foreach(e => asyncCollector.sendPacket(("add", e.identifier), identifier))
    }

    override protected def getAsyncChannel: PacketCollector.Async = {
        if (asyncCollector == null)
            asyncCollector = relay.createAsyncCollector(1)
        asyncCollector
    }


}
