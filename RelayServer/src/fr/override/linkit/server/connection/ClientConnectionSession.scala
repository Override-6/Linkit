package fr.`override`.linkit.server.connection

import java.net.Socket

import fr.`override`.linkit.api.network.{ConnectionState, NetworkEntity, RemoteConsole}
import fr.`override`.linkit.api.packet.traffic.PacketTraffic.SystemChannelID
import fr.`override`.linkit.api.packet.traffic.{ChannelScope, PacketTraffic}
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable, SystemPacketChannel}
import fr.`override`.linkit.server.RelayServer
import fr.`override`.linkit.server.task.ConnectionTasksHandler

case class ClientConnectionSession private(identifier: String,
                                           private val socket: SocketContainer,
                                           server: RelayServer) extends JustifiedCloseable {
    val serverTraffic     : PacketTraffic             = server.traffic
    val channel     : SystemPacketChannel       = serverTraffic.createInjectable(SystemChannelID, ChannelScope.immutable(identifier), SystemPacketChannel)
    val packetReader: ConnectionPacketReader    = new ConnectionPacketReader(socket, server, identifier)
    val tasksHandler: ConnectionTasksHandler    = new ConnectionTasksHandler(this)
    val outConsole  : RemoteConsole             = server.getConsoleOut(identifier)
    val errConsole  : RemoteConsole             = server.getConsoleErr(identifier)

    override def close(reason: CloseReason): Unit = {
        socket.close(reason)
        tasksHandler.close(reason)
        server.network.removeEntity(identifier)
        serverTraffic.close(reason)
    }

    def getSocketState: ConnectionState = socket.getState

    def addStateListener(action: ConnectionState => Unit): Unit = socket.addConnectionStateListener(action)

    def send(bytes: Array[Byte]): Unit = socket.write(bytes)

    def updateSocket(socket: Socket): Unit = this.socket.set(socket)

    def getEntity: NetworkEntity = server.network.getEntity(identifier).get

    override def isClosed: Boolean = socket.isClosed //refers to an used closeable element
}
