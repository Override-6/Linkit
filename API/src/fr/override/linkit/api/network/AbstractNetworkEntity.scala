package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.packet.channel.{AsyncPacketChannel, CommunicationPacketChannel}
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.system.Version
import fr.`override`.linkit.api.utils.cache.{ObjectPacket, SharedCollection}

abstract class AbstractNetworkEntity(relay: Relay, val identifier: String, communicator: CommunicationPacketChannel) extends NetworkEntity {

    protected implicit val traffic: PacketTraffic = relay.traffic
    private lazy val apiVersion: Version = {
        communicator.sendRequest(ObjectPacket("vAPI"))
        communicator.nextResponse(ObjectPacket).casted
    }
    private lazy val relayVersion: Version = {
        communicator.sendRequest(ObjectPacket("vImpl"))
        communicator.nextResponse(ObjectPacket).casted
    }
    protected val remoteFragmentChannel: AsyncPacketChannel = traffic.openChannel(8, identifier, AsyncPacketChannel)

    private val remoteFragments = {
        SharedCollection
                .dedicated(6, identifier)
                .mapped(new RemoteFragmentController(_, remoteFragmentChannel))
    }

    override def addOnStateUpdate(action: ConnectionState => Unit): Unit

    override def getConnectionState: ConnectionState

    override def getProperty(name: String): Serializable = {
        communicator.sendRequest(ObjectPacket(("getProp", name)))
        communicator.nextResponse(ObjectPacket).obj
    }

    override def setProperty(name: String, value: Serializable): Unit = {
        communicator.sendRequest(ObjectPacket(("setProp", name, value)))
    }

    override def getRemoteConsole: RemoteConsole = relay.getConsoleOut(identifier)

    override def getRemoteErrConsole: RemoteConsole = relay.getConsoleErr(identifier)

    override def getApiVersion: Version = apiVersion

    override def getRelayVersion: Version = relayVersion

    override def listRemoteFragmentControllers: List[RemoteFragmentController] = remoteFragments.toList

    override def getRemoteFragmentController(nameIdentifier: String): Option[RemoteFragmentController] = {
        remoteFragments.find(_.nameIdentifier == nameIdentifier)
    }
}
