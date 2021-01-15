package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.{ConnectionState, NetworkEntity}
import fr.`override`.linkit.api.packet.channel.AsyncPacketChannel
import fr.`override`.linkit.api.system.Version
import fr.`override`.linkit.api.utils.cache.SharedCollection

class SelfNetworkEntity(relay: Relay) extends NetworkEntity {

    override val identifier: String = relay.identifier

    private val fragmentHandler = relay.extensionLoader.fragmentHandler
    private val sharedFragments = SharedCollection.open[String](6)(relay.traffic)

    sharedFragments.set(fragmentHandler.listRemoteFragments().map(_.nameIdentifier).toArray)
    fragmentHandler.addOnRemoteFragmentsAdded(sharedFragments.add(_))

    println(s"sharedFragments = ${sharedFragments}")
    sharedFragments.addListener((a, b, c) => println("Fragments modified : " + a, b, c))

    override def addOnStateUpdate(action: ConnectionState => Unit): Unit = relay.addConnectionListener(action)

    override def getConnectionState: ConnectionState = relay.getState

    override def getProperty(name: String): Serializable = relay.properties.get(name).orNull

    override def setProperty(name: String, value: Serializable): Unit = relay.properties.putProperty(name, value)

    override def getRemoteConsole: RemoteConsole = throw new UnsupportedOperationException("Attempted to get a remote console of the current relay")

    override def getRemoteErrConsole: RemoteConsole = throw new UnsupportedOperationException("Attempted to get a remote console of the current relay")

    override def getApiVersion: Version = relay.relayVersion

    override def getRelayVersion: Version = Relay.ApiVersion

    override def listRemoteFragmentControllers: List[RemoteFragmentController] = {
        val fragmentControllerChannel = relay.openChannel(4, identifier, AsyncPacketChannel)

        fragmentHandler
                .listRemoteFragments()
                .map(frag => new RemoteFragmentController(frag.nameIdentifier, fragmentControllerChannel))
    }

    override def getRemoteFragmentController(nameIdentifier: String): Option[RemoteFragmentController] = {
        listRemoteFragmentControllers.find(_.nameIdentifier == nameIdentifier)
    }

    override def toString: String = s"SelfNetworkEntity(identifier: ${relay.identifier})"
}
