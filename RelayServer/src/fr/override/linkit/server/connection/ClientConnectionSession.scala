package fr.`override`.linkit.server.connection

import java.net.Socket

import fr.`override`.linkit.api.network.{ConnectionState, NetworkEntity, RemoteConsole}
import fr.`override`.linkit.api.packet.traffic.DedicatedPacketTraffic
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable, SystemPacketChannel}
import fr.`override`.linkit.server.RelayServer
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

    override def close(reason: CloseReason): Unit = {
        socket.close(reason)
        tasksHandler.close(reason)
        server.network.removeEntity(identifier)
        traffic.close(reason)
    }

    def getSocketState: ConnectionState = socket.getState

    def addStateListener(action: ConnectionState => Unit): Unit = socket.addConnectionStateListener(action)

    def send(bytes: Array[Byte]): Unit = socket.write(bytes)

    def updateSocket(socket: Socket): Unit = this.socket.set(socket)

    def getEntity: NetworkEntity = server.network.getEntity(identifier).get

    override def isClosed: Boolean = socket.isClosed //refers to an used closeable element
}
