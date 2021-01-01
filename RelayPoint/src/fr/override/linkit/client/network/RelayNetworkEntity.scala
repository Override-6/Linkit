package fr.`override`.linkit.client.network

import fr.`override`.linkit.api.packet.channel.PacketChannel.{Async, Sync}
import fr.`override`.linkit.api.packet.fundamental.DataPacket
import fr.`override`.linkit.api.network.{ConnectionState, NetworkEntity}
import fr.`override`.linkit.api.system.{RemoteConsole, Version}
import fr.`override`.linkit.api.utils.Tuple3Packet._
import fr.`override`.linkit.api.utils.{ConsumerContainer, Tuple3Packet}
import fr.`override`.linkit.client.RelayPoint

class RelayNetworkEntity(relay: RelayPoint, async: Async, sync: Sync, val identifier: String) extends NetworkEntity {

    private val (apiVersion, relayVersion) = retrieveVersions()
    private val connectionStateListeners = ConsumerContainer[ConnectionState]()

    @volatile private var connectionState: ConnectionState = ConnectionState.CONNECTED

    override def addOnStateUpdate(action: ConnectionState => Unit): Unit = connectionStateListeners += action

    override def getConnectionState: ConnectionState = connectionState

    override def getStringProperty(name: String): String = {
        async.sendPacket(("getProperty", name))
        sync.nextPacket(DataPacket).contentAsString
    }

    override def setStringProperty(name: String, value: String): String = {
        val before = getStringProperty(name)
        if (name.contains('='))
            throw new IllegalArgumentException("property name cant contain '=' character")
        async.sendPacket(("setProperty", name + '=' + value))
        before
    }

    override def getRemoteConsole: RemoteConsole = relay.getConsoleOut(identifier)

    override def getRemoteErrConsole: RemoteConsole = relay.getConsoleErr(identifier)

    override def getApiVersion: Version = apiVersion

    override def getRelayVersion: Version = relayVersion

    def onConnectionChange(state: ConnectionState): Unit = {
        connectionState = state
        connectionStateListeners.applyAll(state)
    }

    private def retrieveVersions(): (Version, Version) = {
        async.sendPacket(("versions", ""))
        val versions = sync.nextPacket(Tuple3Packet)
        val api = Version.fromString(versions._1)
        val relay = Version.fromString(versions._2)

        (api, relay)
    }

    override def toString: String = s"RelayNetworkEntity(identifier: $identifier, apiVersion: $apiVersion, relayVersion: $relayVersion)"


}