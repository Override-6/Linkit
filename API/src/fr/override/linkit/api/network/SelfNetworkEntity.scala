package fr.`override`.linkit.api.network

import java.sql.Timestamp

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.cache.SharedCacheHandler
import fr.`override`.linkit.api.network.{ConnectionState, NetworkEntity}
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.collector.CommunicationPacketCollector
import fr.`override`.linkit.api.system.Version

class SelfNetworkEntity(relay: Relay) extends NetworkEntity {

    override val identifier: String = relay.identifier

    override val cache: SharedCacheHandler = SharedCacheHandler.create(identifier, identifier)(relay.traffic)
    cache.post(4, Relay.ApiVersion)
    cache.post(5, relay.relayVersion)

    private val fragmentHandler = relay.extensionLoader.fragmentHandler

    override val connectionDate: Timestamp = cache.post(2, new Timestamp(System.currentTimeMillis()))

    override def addOnStateUpdate(action: ConnectionState => Unit): Unit = relay.addConnectionListener(action)

    override def getConnectionState: ConnectionState = relay.getConnectionState

    override def getProperty(name: String): Serializable = relay.properties.get(name).orNull

    override def setProperty(name: String, value: Serializable): Unit = relay.properties.putProperty(name, value)

    override def getRemoteConsole: RemoteConsole = throw new UnsupportedOperationException("Attempted to get a remote console of the current relay")

    override def getRemoteErrConsole: RemoteConsole = throw new UnsupportedOperationException("Attempted to get a remote console of the current relay")

    override def getApiVersion: Version = Relay.ApiVersion

    override def getRelayVersion: Version = relay.relayVersion

    override def listRemoteFragmentControllers: List[RemoteFragmentController] = {
        val communicator = relay
                .openCollector(4, CommunicationPacketCollector.providable)
                .subChannel(identifier, CommunicationPacketChannel.providable, true)

        fragmentHandler
                .listRemoteFragments()
                .map(frag => new RemoteFragmentController(frag.nameIdentifier, communicator))
    }

    override def getFragmentController(nameIdentifier: String): Option[RemoteFragmentController] = {
        listRemoteFragmentControllers.find(_.nameIdentifier == nameIdentifier)
    }

    override def toString: String = s"SelfNetworkEntity(identifier: ${relay.identifier})"

}
