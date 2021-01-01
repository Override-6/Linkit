package fr.`override`.linkit.server.connection

import java.net.Socket

import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.packet.channel.SyncPacketChannel
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates, SimpleTrafficHandler, TrafficHandler}
import fr.`override`.linkit.api.packet.fundamental.DataPacket
import fr.`override`.linkit.api.network.{ConnectionState, NetworkEntity}
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable, RemoteConsole, SystemOrder, SystemPacket, SystemPacketChannel}
import fr.`override`.linkit.api.task.TasksHandler
import fr.`override`.linkit.server.RelayServer
import fr.`override`.linkit.server.exceptions.ConnectionInitialisationException
import fr.`override`.linkit.server.task.ConnectionTasksHandler

import scala.collection.mutable.ListBuffer

case class ClientConnectionSession private(identifier: String,
                                           private val socket: SocketContainer,
                                           server: RelayServer,
                                           outConsole: RemoteConsole,
                                           errConsole: RemoteConsole.Err,
                                           entity: NetworkEntity,
                                           traffic: TrafficHandler) extends JustifiedCloseable {


  val channel: SystemPacketChannel = new SystemPacketChannel(identifier, traffic)
  val packetReader = new ConnectionPacketReader(socket, server, identifier)
  val tasksHandler = new ConnectionTasksHandler(this)

  override def close(reason: CloseReason): Unit = {
    if (tasksHandler != null)
      tasksHandler.close(reason)
    socket.close(reason)
    traffic.close(reason)
  }

  def getSocketState: ConnectionState = socket.getState

  def addStateListener(action: ConnectionState => Unit): Unit = socket.addConnectionStateListener(action)

  def send(bytes: Array[Byte]): Unit = socket.write(bytes)

  def updateSocket(socket: Socket): Unit = this.socket.set(socket)


}

object ClientConnectionSession {
  def open(identifier: String,
           socket: SocketContainer,
           server: RelayServer,
           trafficHandler: TrafficHandler): ClientConnectionSession = {


    val outConsole = server.getConsoleOut(identifier).orNull
    val errConsole = server.getConsoleErr(identifier).orNull
    if (outConsole == null || errConsole == null)
      throw new ConnectionInitialisationException(s"Unable to retrieve consoles for $identifier")

    val network = server.network
    network.addEntity(identifier)
    val entity = network.getEntity(identifier).orNull
    if (entity == null)
      throw new ConnectionInitialisationException(s"Something went wrong when registering this connection session to the network")

    val session = new ClientConnectionSession(identifier, socket, server, outConsole, errConsole, entity, trafficHandler)
    session
  }

}
