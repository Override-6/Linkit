package fr.`override`.linkit.server.connection

import fr.`override`.linkit.internal.concurrency.relayWorkerExecution
import fr.`override`.linkit.skull.connection.network.{ConnectionState, NetworkEntity, RemoteConsole}
import fr.`override`.linkit.skull.connection.packet.traffic.PacketTraffic.SystemChannelID
import fr.`override`.linkit.skull.connection.packet.traffic.{ChannelScope, PacketTraffic}
import fr.`override`.linkit.skull.internal.system.{CloseReason, JustifiedCloseable, SystemPacketChannel}
import fr.`override`.linkit.server.RelayServer
import fr.`override`.linkit.server.task.ConnectionTasksHandler
import java.net.Socket
import fr.`override`.linkit.skull.internal.system.event.packet.PacketEvents

case class ClientConnectionSession private(identifier: String,
                                           private val socket: SocketContainer,
                                           server: RelayServer) extends JustifiedCloseable {

    val serverTraffic: PacketTraffic             = server traffic
    val channel      : SystemPacketChannel       = serverTraffic.getInjectable(SystemChannelID, ChannelScope.reserved(identifier), SystemPacketChannel)
    val packetReader : ConnectionPacketReader    = new ConnectionPacketReader(socket, server, identifier)
    val tasksHandler : ConnectionTasksHandler    = new ConnectionTasksHandler(this)
    val outConsole   : RemoteConsole             = server.getConsoleOut(identifier)
    val errConsole   : RemoteConsole             = server.getConsoleErr(identifier)

    @relayWorkerExecution
    override def close(reason: CloseReason): Unit = {
        socket.close(reason)
        tasksHandler.close(reason)
        server.network.removeEntity(identifier)
        serverTraffic.close(reason)
    }
    def getSocketState: ConnectionState = socket.getState

    def send(result: PacketSerializationResult): Unit = {
        socket.write(result.writableBytes())
        val event = PacketEvents.packetWritten(result)
        server.eventNotifier.notifyEvent(server.packetHooks, event)
    }

    def send(bytes: Array[Byte]): Unit = {
        socket.write(NumberSerializer.serializeInt(bytes.length) ++ bytes)
    }

    def updateSocket(socket: Socket): Unit = this.socket.set(socket)

    def getEntity: NetworkEntity = server.network.getEntity(identifier).get

    override def isClosed: Boolean = socket.isClosed //refers to an used closeable element
}
