package fr.`override`.linkit.core.connection.network

import fr.`override`.linkit.skull.Relay
import fr.`override`.linkit.skull.connection.network.cache.SharedCacheHandler
import fr.`override`.linkit.skull.connection.network.{ConnectionState, NetworkEntity}
import fr.`override`.linkit.skull.connection.packet.traffic.ChannelScope
import fr.`override`.linkit.skull.connection.packet.traffic.channel.{CommunicationPacketChannel, PacketChannelCategories}
import fr.`override`.linkit.skull.internal.system.Version
import java.sql.Timestamp

import fr.`override`.linkit.core.connection.network
import fr.`override`.linkit.core.connection.packet.traffic
import fr.`override`.linkit.core.connection.packet.traffic.channel

class SelfNetworkEntity(relay: Relay, override val cache: cache.SharedCacheHandler) extends NetworkEntity {

    override val identifier: String = relay.identifier

    override val connectionDate: Timestamp = new Timestamp(System.currentTimeMillis())

    override val apiVersion: Version = Relay.ApiVersion

    override val relayVersion: Version = relay.relayVersion
    update()

    override def update(): this.type = {
        cache.post(2, connectionDate)
        cache.post(4, apiVersion)
        cache.post(5, relayVersion)
        cache.update()
        this
    }

    override def getConnectionState: network.ConnectionState = relay.getConnectionState

    override def getProperty(name: String): Serializable = relay.properties.get(name).orNull

    override def setProperty(name: String, value: Serializable): Unit = relay.properties.putProperty(name, value)

    override def getRemoteConsole: SimpleRemoteConsole = throw new UnsupportedOperationException("Attempted to get a remote console of the current relay")

    override def getRemoteErrConsole: SimpleRemoteConsole = throw new UnsupportedOperationException("Attempted to get a remote console of the current relay")

    override def listRemoteFragmentControllers: List[RemoteFragmentController] = {
        val communicator = relay
                .getInjectable(4, ChannelScope.broadcast, traffic.channel.PacketChannelCategories)
                .subInjectable(identifier, channel.CommunicationPacketChannel.providable, true)

        val fragmentHandler = relay.extensionLoader.fragmentHandler
        fragmentHandler
                .listRemoteFragments()
                .map(frag => new RemoteFragmentController(frag.nameIdentifier, communicator))
    }

    override def getFragmentController(nameIdentifier: String): Option[RemoteFragmentController] = {
        listRemoteFragmentControllers.find(_.nameIdentifier == nameIdentifier)
    }

    override def toString: String = s"SelfNetworkEntity(identifier: ${relay.identifier})"

}
