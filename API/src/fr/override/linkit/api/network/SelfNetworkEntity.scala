package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.{ConnectionState, NetworkEntity}
import fr.`override`.linkit.api.packet.channel.PacketChannel
import fr.`override`.linkit.api.system.Version

class SelfNetworkEntity(relay: Relay, packetChannel: PacketChannel.Async) extends NetworkEntity {

    override val identifier: String = relay.identifier

    override def addOnStateUpdate(action: ConnectionState => Unit): Unit = relay.addConnectionListener(action)

    override def getConnectionState: ConnectionState = relay.getState

    override def getStringProperty(name: String): String = relay.properties.get(name).getOrElse("")

    override def setStringProperty(name: String, value: String): String =
        String.valueOf(relay.properties.putProperty(name, value))

    override def getRemoteConsole: RemoteConsole = throw new UnsupportedOperationException("Attempted to get a remote console of the current relay")

    override def getRemoteErrConsole: RemoteConsole = throw new UnsupportedOperationException("Attempted to get a remote console of the current relay")

    override def getApiVersion: Version = relay.relayVersion

    override def getRelayVersion: Version = Relay.ApiVersion

    override def toString: String = s"SelfNetworkEntity(identifier: ${relay.identifier})"

    /*override def listRemoteFragmentControllers: List[RemoteFragmentController] = {
        relay
                .extensionLoader
                .fragmentHandler
                .listRemoteFragments()
                .map(frag => new RemoteFragmentController(frag.nameIdentifier, packetChannel))
    }

    override def getRemoteFragmentController(nameIdentifier: String): Option[RemoteFragmentController] = {
        listRemoteFragmentControllers.find(_.nameIdentifier == nameIdentifier)
    }

    /*
     * The following implementations are useless,
     * because the connection state or remote fragments are already handled and updated due to handling data
     * of the current relay.
     * However, the implementation still required in order to fit into entity collections
     * */
    override def setConnectionState(state: ConnectionState): Unit = ()

    override def addRemoteFragments(names: Array[String]): Unit = ()*/
}
