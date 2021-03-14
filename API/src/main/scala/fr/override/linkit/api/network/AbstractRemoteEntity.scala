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
                                    override val cache: SharedCacheHandler,
                                    private val communicator: CommunicationPacketChannel) extends NetworkEntity {

    println(s"CREATING REMOTE ENTITY NAMED '$identifier'")
    protected implicit val traffic: PacketTraffic = relay.traffic

    println("Cache created !")
    private val remoteFragments = {
        val communicator = traffic
                .getInjectable(4, ChannelScope.broadcast, PacketChannelCategories)
                .subInjectable(Array(identifier), PacketChannelCategories, true)

        cache
                .get(6, SharedCollection.set[String])
                .mapped(name => new RemoteFragmentController(name, communicator.createCategory(name, ChannelScope.broadcast, CommunicationPacketChannel)))
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

    override def getRemoteConsole: RemoteConsole = relay.getConsoleOut(identifier)

    override def getRemoteErrConsole: RemoteConsole = relay.getConsoleErr(identifier)

    override def listRemoteFragmentControllers: List[RemoteFragmentController] = remoteFragments.toList

    override def getFragmentController(nameIdentifier: String): Option[RemoteFragmentController] = {
        remoteFragments.find(_.nameIdentifier == nameIdentifier)
    }

    override def toString: String = s"${getClass.getSimpleName}(identifier: $identifier, state: $getConnectionState, api: $apiVersion, impl: $relayVersion)"
}
