package fr.`override`.linkit.api.network

import java.sql.Timestamp

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.cache.collection.{BoundedCollection, SharedCollection}
import fr.`override`.linkit.api.network.cache.{ObjectPacket, SharedCacheHandler}
import fr.`override`.linkit.api.packet.channel.CommunicationPacketChannel
import fr.`override`.linkit.api.packet.collector.CommunicationPacketCollector
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.system.Version

abstract class AbstractRemoteEntity(private val relay: Relay,
                                    override val identifier: String,
                                    private val communicator: CommunicationPacketChannel) extends NetworkEntity {

    //println(s"CREATED REMOTE ENTITY NAMED '$identifier'")
    protected implicit val traffic: PacketTraffic = relay.traffic
    private lazy val apiVersion: Version = {
        communicator.sendRequest(ObjectPacket("vAPI"))
        communicator.nextResponse(ObjectPacket).casted
    }

    private lazy val relayVersion: Version = {
        communicator.sendRequest(ObjectPacket("vImpl"))
        communicator.nextResponse(ObjectPacket).casted
    }

    override val cache: SharedCacheHandler = SharedCacheHandler.create(identifier, identifier)
    override val connectionDate: Timestamp = cache(2)
    private val remoteFragments = {
        val communicator = traffic
                .openCollector(4, CommunicationPacketCollector)
                .subChannel(identifier, CommunicationPacketChannel, true)

        var c: BoundedCollection.Immutable[RemoteFragmentController] = null
        c = cache
                .open(6, SharedCollection.set[String])
                .mapped(new RemoteFragmentController(_, communicator))
        c
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

    override def getFragmentController(nameIdentifier: String): Option[RemoteFragmentController] = {
        println(s"remoteFragments = $remoteFragments")
        remoteFragments.find(_.nameIdentifier == nameIdentifier)
    }

    override def toString: String = s"${getClass.getSimpleName}(identifier: $identifier, state: $getConnectionState)"
}
