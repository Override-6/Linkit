package fr.`override`.linkit.server.network

import fr.`override`.linkit.api.packet.collector.PacketCollector
import fr.`override`.linkit.api.packet.fundamental.DataPacket
import fr.`override`.linkit.api.network.{ConnectionState, NetworkEntity}
import fr.`override`.linkit.api.system.{RemoteConsole, Version}
import fr.`override`.linkit.api.utils.Tuple3Packet
import fr.`override`.linkit.api.utils.Tuple3Packet._
import fr.`override`.linkit.server.connection.ClientConnection

class ConnectionNetworkEntity(async: PacketCollector.Async,
                              sync: PacketCollector.Sync,
                              connection: ClientConnection) extends NetworkEntity {

    override val identifier: String = connection.identifier

    override def addOnStateUpdate(action: ConnectionState => Unit): Unit = connection.addConnectionStateListener(action)

    override def getConnectionState: ConnectionState = connection.getState

    override def getStringProperty(name: String): String = {
        async.sendPacket(("getProperty", name), identifier)
        sync.nextPacket(classOf[DataPacket]).contentAsString
    }

    override def setStringProperty(name: String, value: String): String = {
        val before = getStringProperty(name)
        async.sendPacket(("setProperty", name, value), identifier)
        before
    }

    override def getRemoteConsole: RemoteConsole = connection.getConsoleOut

    override def getRemoteErrConsole: RemoteConsole = connection.getConsoleErr

    override def getApiVersion: Version = apiVersion

    override def getRelayVersion: Version = relayVersion

    private val (apiVersion, relayVersion) = retrieveVersions()

    private def retrieveVersions(): (Version, Version) = {
        async.sendPacket(("versions", ""), identifier)
        val versions = sync.nextPacket(classOf[Tuple3Packet])
        val api = Version.fromString(versions._1)
        val relay = Version.fromString(versions._2)

        (api, relay)
    }

    override def toString: String = s"ConnectionNetworkEntity(identifier: $identifier, apiVersion: $apiVersion, relayVersion: $relayVersion)"


}

