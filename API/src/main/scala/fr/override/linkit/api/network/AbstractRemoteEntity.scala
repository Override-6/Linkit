package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.cache.SharedCacheHandler
import fr.`override`.linkit.api.network.cache.collection.SharedCollection
import fr.`override`.linkit.api.packet.fundamental.RefPacket.ObjectPacket
import fr.`override`.linkit.api.packet.traffic.channel.{CommunicationPacketChannel, PacketChannelCategories}
import fr.`override`.linkit.api.packet.traffic.{ChannelScope, PacketTraffic}
import fr.`override`.linkit.api.system.Version
import fr.`override`.linkit.api.system.evente.network.NetworkEvents

import java.sql.Timestamp

abstract class AbstractRemoteEntity(private val relay: Relay,
                                    override val identifier: String,
                                    private val communicator: CommunicationPacketChannel) extends NetworkEntity {

    //println(s"CREATED REMOTE ENTITY NAMED '$identifier'")
    protected implicit val traffic: PacketTraffic = relay.traffic

    override val cache: SharedCacheHandler = SharedCacheHandler.create(identifier, identifier)
    private val remoteFragments = {
        val communicator = traffic
                .getInjectable(4, ChannelScope.broadcast, PacketChannelCategories)
                .subInjectable(Array(identifier), PacketChannelCategories, true)

        cache
                .get(6, SharedCollection.set[String])
                .mapped(name => new RemoteFragmentController(name, communicator.createCategory(name, ChannelScope.broadcast, CommunicationPacketChannel)))
    }
    override val connectionDate: Timestamp = cache(2)

    override val apiVersion: Version = cache(4)

    override val relayVersion: Version = cache(5)

    override def getConnectionState: ConnectionState

    override def getProperty(name: String): Serializable = {
        communicator.sendRequest(ObjectPacket(("getProp", name)))
        communicator.nextResponse[ObjectPacket].casted
    }

    override def setProperty(name: String, newValue: Serializable): Unit = {
        val eventHandlingEnabled = relay.configuration.enableEventHandling

        def update(): Unit = communicator.sendRequest(ObjectPacket(("setProp", name, newValue)))

        if (!eventHandlingEnabled) {
            update()
            return
        }
        val oldValue = getProperty(name)
        update()
        if (!eventHandlingEnabled)
            return

        val event = NetworkEvents.remotePropertyChange(this, name, newValue, oldValue)
        relay.eventNotifier.notifyEvent(relay.networkHooks, event)
    }

    override def getRemoteConsole: RemoteConsole = relay.getConsoleOut(identifier)

    override def getRemoteErrConsole: RemoteConsole = relay.getConsoleErr(identifier)

    override def listRemoteFragmentControllers: List[RemoteFragmentController] = remoteFragments.toList

    override def getFragmentController(nameIdentifier: String): Option[RemoteFragmentController] = {
        remoteFragments.find(_.nameIdentifier == nameIdentifier)
    }

    override def toString: String = s"${getClass.getSimpleName}(identifier: $identifier, state: $getConnectionState, api: $apiVersion, impl: $relayVersion)"
}
