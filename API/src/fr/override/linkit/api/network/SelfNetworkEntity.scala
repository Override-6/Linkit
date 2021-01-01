package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.system.{RemoteConsole, Version}
import fr.`override`.linkit.api.network.{ConnectionState, NetworkEntity}

class SelfNetworkEntity(relay: Relay) extends NetworkEntity {
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
}
