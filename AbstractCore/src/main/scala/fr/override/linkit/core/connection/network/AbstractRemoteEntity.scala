package fr.`override`.linkit.core.connection.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.connection.network.cache.SharedCacheHandler
import fr.`override`.linkit.api.connection.network.cache.collection.SharedCollection
import fr.`override`.linkit.api.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.`override`.linkit.api.connection.packet.traffic.channel.{CommunicationPacketChannel, PacketChannelCategories}
import fr.`override`.linkit.api.connection.packet.traffic.{ChannelScope, PacketTraffic}
import fr.`override`.linkit.api.local.system.Version
import fr.`override`.linkit.api.local.system.event.network.NetworkEvents
import java.sql.Timestamp

import fr.`override`.linkit.core.connection.network.cache.collection
import fr.`override`.linkit.core.connection.packet
import fr.`override`.linkit.core.connection.packet.traffic
import fr.`override`.linkit.core.connection.packet.traffic.channel

abstract class AbstractRemoteEntity(private val relay: Relay,
                                    override val identifier: String,
                                    override val cache: cache.SharedCacheHandler,
                                    private val communicator: packet.traffic.channel.CommunicationPacketChannel) extends NetworkEntity {

    println(s"CREATING REMOTE ENTITY NAMED '$identifier'")
    protected implicit val traffic: PacketTraffic = relay.traffic

    println("Cache created !")
    private val remoteFragments = {
        val communicator = traffic
                .getInjectable(4, ChannelScope.broadcast, traffic.channel.PacketChannelCategories)
                .subInjectable(Array(identifier), traffic.channel.PacketChannelCategories, true)

        cache
                .get(6, collection.SharedCollection.set[String])
                .mapped(name => new RemoteFragmentController(name, communicator.createCategory(name, ChannelScope.broadcast, channel.CommunicationPacketChannel)))
    }
    println("RemoteFragment created !")

    override def connectionDate: Timestamp = cache.getOrWait(2)

    override def apiVersion: Version = cache.getOrWait(4)

    override def relayVersion: Version = cache.getOrWait(5)

    override def update(): this.type = {
        cache.update()
        this
    }

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

    override def getRemoteConsole: SimpleRemoteConsole = relay.getConsoleOut(identifier)

    override def getRemoteErrConsole: SimpleRemoteConsole = relay.getConsoleErr(identifier)

    override def listRemoteFragmentControllers: List[RemoteFragmentController] = remoteFragments.toList

    override def getFragmentController(nameIdentifier: String): Option[RemoteFragmentController] = {
        remoteFragments.find(_.nameIdentifier == nameIdentifier)
    }

    override def toString: String = s"${getClass.getSimpleName}(identifier: $identifier, state: $getConnectionState, api: $apiVersion, impl: $relayVersion)"
}
