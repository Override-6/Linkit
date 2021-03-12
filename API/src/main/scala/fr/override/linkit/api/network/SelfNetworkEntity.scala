package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.cache.{SharedCacheHandler, SharedInstance}
import fr.`override`.linkit.api.network.{ConnectionState, NetworkEntity}
import fr.`override`.linkit.api.packet.traffic.ChannelScope
import fr.`override`.linkit.api.packet.traffic.channel.{CommunicationPacketChannel, PacketChannelCategories}
import fr.`override`.linkit.api.system.Version

import java.sql.Timestamp

class SelfNetworkEntity(relay: Relay) extends NetworkEntity {

    override val identifier: String = relay.identifier

    override val cache: SharedCacheHandler = SharedCacheHandler.create(identifier, identifier)(relay.traffic)
    cache.post(4, Relay.ApiVersion)
    cache.post(5, relay.relayVersion)

    private val sharedState = cache.get(3, SharedInstance[ConnectionState])
    //TODO ConnectionStateChange !!!
    //relay.relayHooks.stateChange.add(t => sharedState.set(t.state))
    override val apiVersion: Version = Relay.ApiVersion

    override val relayVersion: Version = relay.relayVersion

    override val connectionDate: Timestamp = cache.post(2, new Timestamp(System.currentTimeMillis()))

    override def getConnectionState: ConnectionState = relay.getConnectionState

    override def getProperty(name: String): Serializable = relay.properties.get(name).orNull

    override def setProperty(name: String, value: Serializable): Unit = relay.properties.putProperty(name, value)

    override def getRemoteConsole: RemoteConsole = throw new UnsupportedOperationException("Attempted to get a remote console of the current relay")

    override def getRemoteErrConsole: RemoteConsole = throw new UnsupportedOperationException("Attempted to get a remote console of the current relay")

    override def listRemoteFragmentControllers: List[RemoteFragmentController] = {
        val communicator = relay
            .getInjectable(4, ChannelScope.broadcast, PacketChannelCategories)
            .subInjectable(identifier, CommunicationPacketChannel.providable, true)

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
