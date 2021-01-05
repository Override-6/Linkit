package fr.`override`.linkit.server.connection

import java.net.Socket

import fr.`override`.linkit.api.network.{ConnectionState, NetworkEntity}
import fr.`override`.linkit.api.packet.traffic.DedicatedPacketTraffic
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable, RemoteConsole, SystemPacketChannel}
import fr.`override`.linkit.server.RelayServer
import fr.`override`.linkit.server.exceptions.ConnectionInitialisationException
import fr.`override`.linkit.server.task.ConnectionTasksHandler

case class ClientConnectionSession private(identifier: String,
                                           private val socket: SocketContainer,
                                           server: RelayServer) extends JustifiedCloseable {

    val traffic = new DedicatedPacketTraffic(server, socket, identifier)
    val channel: SystemPacketChannel = new SystemPacketChannel(identifier, traffic)
    val packetReader = new ConnectionPacketReader(socket, server, identifier)
    val tasksHandler = new ConnectionTasksHandler(this)
    val outConsole: RemoteConsole = server.getConsoleOut(identifier)
    val errConsole: RemoteConsole = server.getConsoleErr(identifier)
    private var entity: NetworkEntity = _ //Can't be a val because the NetworkEntity initialisation needs the connection to be registered and started

    override def close(reason: CloseReason): Unit = {
        tasksHandler.close(reason)
        traffic.close(reason)
        server.network.removeEntity(identifier)
        socket.close(reason)
    }

    def getSocketState: ConnectionState = socket.getState

    def addStateListener(action: ConnectionState => Unit): Unit = socket.addConnectionStateListener(action)

    def send(bytes: Array[Byte]): Unit = socket.write(bytes)

    def updateSocket(socket: Socket): Unit = this.socket.set(socket)

    def getEntity: NetworkEntity = entity

    private[connection] def initNetwork(): Unit = {
        val network = server.network
        network.addEntity(identifier)
        entity = network.getEntity(identifier).orNull

        if (entity == null)
            throw new ConnectionInitialisationException("Something went wrong when registering this connection session to the network")
    }

    override def isClosed: Boolean = socket.isClosed //refers to an used closeable element
}
