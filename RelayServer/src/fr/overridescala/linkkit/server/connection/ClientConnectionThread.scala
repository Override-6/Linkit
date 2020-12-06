package fr.overridescala.linkkit.server.connection

import java.net.Socket

import fr.overridescala.linkkit.api.exceptions.{RelayException, UnexpectedPacketException}
import fr.overridescala.linkkit.api.packet._
import fr.overridescala.linkkit.api.packet.channel.{AsyncPacketChannel, PacketChannel, SyncPacketChannel}
import fr.overridescala.linkkit.api.packet.fundamental._
import fr.overridescala.linkkit.api.system._
import fr.overridescala.linkkit.api.task.TasksHandler
import fr.overridescala.linkkit.server.RelayServer
import fr.overridescala.linkkit.server.task.ConnectionTasksHandler

import scala.util.control.NonFatal

class ClientConnectionThread private(socket: SocketContainer,
                                     server: RelayServer,
                                     val identifier: String) extends Thread with JustifiedCloseable {


    private val packetManager = server.packetManager
    private val notifier = server.eventObserver.notifier
    private val manager: ConnectionsManager = server.connectionsManager

    private val connectionTraffic = new SimpleTrafficHandler(notifier, socket, server.identifier, packetManager)
    private val serverTraffic = server.trafficHandler

    val systemChannel: SystemPacketChannel = new SystemPacketChannel(identifier, connectionTraffic)

    @volatile private var tasksHandler: TasksHandler = _
    @volatile private var closed = false
    private var remoteConsoleErr: RemoteConsole.Err = _

    def load(): Unit = {
        remoteConsoleErr = server.getConsoleErr(identifier).orNull
        tasksHandler = new ConnectionTasksHandler(identifier, server, systemChannel, remoteConsoleErr)

        val remoteConsoleOut = server.getConsoleOut(identifier).orNull
        remoteConsoleOut.print(s"Connected to server ${server.relayVersion} (${server.apiVersion})")
    }

    override def run(): Unit = {
        if (closed)
            throw new RelayException("This Connection was already used and is now definitely closed")

        load()
        println(s"Thread '$getName' was started")

        try {
            val packetReader = new ServerPacketReader(socket, server, identifier)
            while (!closed)
                packetReader.nextPacket(handlePacket)
        } catch {
            case NonFatal(e) =>
                remoteConsoleErr.reportException(e)
                e.printStackTrace()
        } finally {
            close(Reason.INTERNAL_ERROR)
        }
        println(s"End of Thread execution '$getName'")
    }

    override def close(reason: Reason): Unit = {
        println(s"Closing thread '$getName'")
        if (socket.isConnected && reason.isInternal) {
            systemChannel.sendOrder(SystemOrder.CLIENT_CLOSE, reason)
        }

        tasksHandler.close(reason)
        socket.close(reason)
        connectionTraffic.close(reason)
        manager.unregister(identifier)

        closed = true
        println(s"Thread '$getName' closed.")
    }

    def isConnected: Boolean = socket.isConnected

    def getTasksHandler: TasksHandler = tasksHandler

    def createSync(id: Int): PacketChannel.Sync =
        new SyncPacketChannel(identifier, id, connectionTraffic)

    def createAsync(id: Int): PacketChannel.Async = {
        new AsyncPacketChannel(identifier, id, connectionTraffic)
    }

    def sendPacket(packet: Packet, channelID: Int): Unit = {
        socket.write(packetManager.toBytes(packet, PacketCoordinates(channelID, identifier, server.identifier)))
    }

    private[server] def updateSocket(socket: Socket): Unit =
        this.socket.set(socket)

    private[connection] def sendDeflectedBytes(bytes: Array[Byte]): Unit = {
        socket.write(PacketUtils.wrap(bytes))
    }

    private def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        val identifier = coordinates.containerID

        notifier.onPacketReceived(packet, coordinates)
        packet match {
            case systemError: ErrorPacket if identifier == systemChannel.identifier => systemError.printError()
            case systemPacket: SystemPacket => handleSystemOrder(systemPacket)
            case init: TaskInitPacket => tasksHandler.handlePacket(init, coordinates)
            case _: Packet =>
                if (serverTraffic.isTargeted(coordinates)) {
                    serverTraffic.injectPacket(packet, coordinates)
                    return
                }
                connectionTraffic.injectPacket(packet, coordinates)
        }
    }


    private def handleSystemOrder(packet: SystemPacket): Unit = {
        val orderType = packet.order
        val reason = packet.reason.reversed()
        val content = packet.content

        notifier.onSystemOrderReceived(orderType, reason)
        orderType match {
            case SystemOrder.CLIENT_CLOSE => close(reason)
            case SystemOrder.SERVER_CLOSE => server.close(identifier, reason)
            case SystemOrder.ABORT_TASK => tasksHandler.skipCurrent(reason)
            case SystemOrder.CHECK_ID => checkIDRegistered(new String(content))

            case _ => systemChannel.sendPacket(ErrorPacket("forbidden order", s"could not complete order '$orderType', can't be handled by a server or unknown order"))
        }
    }

    private def checkIDRegistered(target: String): Unit = {
        val response = if (manager.containsIdentifier(target)) "OK" else "ERROR"
        systemChannel.sendPacket(DataPacket(response))
    }

    setName(s"RP Connection ($identifier)")

}

object ClientConnectionThread {

    def open(socket: SocketContainer, server: RelayServer, identifier: String = null): ClientConnectionThread = {
        val relayIdentifier = if (identifier == null) retrieveIdentifier(socket, server) else identifier
        val connection = new ClientConnectionThread(socket, server, relayIdentifier)
        connection.start()
        connection
    }

    def retrieveIdentifier(socket: SocketContainer, server: RelayServer): String = {
        val packetReader = new ServerPacketReader(socket, server, null)
        val tempHandler = new SimpleTrafficHandler(server.notifier, socket, "unknown", server.packetManager)

        val channel: SyncPacketChannel =
            new SyncPacketChannel("unknown", 6, tempHandler)

        def deflect(): Unit = packetReader.nextPacket {
            case (concerned: Packet, coords: PacketCoordinates) if coords.containerID == channel.identifier =>
                channel.injectPacket(concerned, coords)
            case _ => deflect()
        }

        def handleClientResponse(): String = channel.nextPacket() match {
            case dataPacket: DataPacket => dataPacket.header
            case other =>
                val name = other.getClass.getSimpleName
                throw new UnexpectedPacketException(s"Unexpected packet type $name received while getting RelayPoint identifier.")
        }

        channel.sendPacket(SystemPacket(SystemOrder.GET_IDENTIFIER, Reason.INTERNAL))
        deflect()

        val identifier = handleClientResponse()
        channel.close(Reason.INTERNAL)

        identifier
    }

}
