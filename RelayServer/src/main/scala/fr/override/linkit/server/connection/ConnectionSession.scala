package fr.`override`.linkit.server.connection

import fr.`override`.linkit.api.connection.network.{ConnectionState, NetworkEntity}
import fr.`override`.linkit.api.connection.packet.serialization.PacketSerializationResult
import fr.`override`.linkit.api.connection.packet.traffic.PacketTraffic.SystemChannelID
import fr.`override`.linkit.api.connection.packet.traffic.{ChannelScope, PacketTraffic}
import fr.`override`.linkit.api.local.concurrency.workerExecution
import fr.`override`.linkit.api.local.system.{CloseReason, JustifiedCloseable}
import fr.`override`.linkit.core.connection.packet.serialization.NumberSerializer
import fr.`override`.linkit.core.local.system.SystemPacketChannel
import fr.`override`.linkit.core.local.system.event.packet.PacketEvents
import fr.`override`.linkit.server.task.ConnectionTasksHandler

import java.net.Socket

case class ConnectionSession private(identifier: String,
                                     private val socket: SocketContainer,
                                     server: ServerConnection) extends JustifiedCloseable {

    val serverTraffic: PacketTraffic             = server traffic
    val channel      : SystemPacketChannel       = serverTraffic.getInjectable(SystemChannelID, ChannelScope.reserved(identifier), SystemPacketChannel)
    val packetReader : ConnectionPacketReader    = new ConnectionPacketReader(socket, server, identifier)
    val tasksHandler : ConnectionTasksHandler    = new ConnectionTasksHandler(this)

    @workerExecution
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
        //server.eventNotifier.notifyEvent(server.packetHooks, event)
    }

    def send(bytes: Array[Byte]): Unit = {
        socket.write(NumberSerializer.serializeInt(bytes.length) ++ bytes)
    }

    def updateSocket(socket: Socket): Unit = this.socket.set(socket)

    def getEntity: NetworkEntity = server.network.getEntity(identifier).get

    override def isClosed: Boolean = socket.isClosed //refers to an used closeable element
}
