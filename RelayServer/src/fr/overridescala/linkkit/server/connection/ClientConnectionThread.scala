package fr.overridescala.linkkit.server.connection

import java.net.Socket

import fr.overridescala.linkkit.api.exceptions.{RelayException, UnexpectedPacketException}
import fr.overridescala.linkkit.api.packet._
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
    private val channelsHandler = new PacketChannelsHandler(notifier, socket, packetManager)

    val systemChannel: SystemPacketChannel = new SystemPacketChannel(identifier, server.identifier, channelsHandler)

    private val manager: ConnectionsManager = server.connectionsManager

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
        channelsHandler.close(reason)
        manager.unregister(identifier)

        closed = true
        println(s"Thread '$getName' closed.")
    }

    def isConnected: Boolean = socket.isConnected

    def getTasksHandler: TasksHandler = tasksHandler

    private[server] def createSync(id: Int): SyncPacketChannel =
        new SyncPacketChannel(identifier, server.identifier, id, channelsHandler)

    private[server] def createAsync(id: Int): AsyncPacketChannel = {
        new AsyncPacketChannel(identifier, server.identifier, id, channelsHandler)
    }

    private[server] def updateSocket(socket: Socket): Unit =
        this.socket.set(socket)

    private[connection] def sendDeflectedBytes(bytes: Array[Byte]): Unit = {
        socket.write(PacketUtils.wrap(bytes))
    }

    private def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        val channelID = coordinates.channelID

        notifier.onPacketReceived(packet, coordinates)
        packet match {
            case systemError: ErrorPacket if channelID == systemChannel.channelID => systemError.printError()
            case systemPacket: SystemPacket => handleSystemOrder(systemPacket)
            case init: TaskInitPacket => tasksHandler.handlePacket(init, coordinates)
            case _: Packet => channelsHandler.injectPacket(packet, channelID)
        }
    }


    private def handleSystemOrder(packet: SystemPacket): Unit = {
        val orderType = packet.order
        val reason = packet.reason
        val content = packet.content

        notifier.onSystemOrderReceived(orderType, reason)
        orderType match {
            case SystemOrder.CLIENT_CLOSE => close(reason)
            case SystemOrder.SERVER_CLOSE => server.close(identifier, reason)
            case SystemOrder.ABORT_TASK => tasksHandler.skipCurrent(reason)
            case SystemOrder.CHECK_ID => checkIDRegistered(new String(content))
            case SystemOrder.LINK_CONSOLE_OUT => server.remoteConsoles.linkOut(identifier, new String(content).toInt)
            case SystemOrder.LINK_CONSOLE_ERR => server.remoteConsoles.linkErr(identifier, new String(content).toInt)

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
        val tempChannelsHandler = new PacketChannelsHandler(server.notifier, socket, server.packetManager)

        val channel: SyncPacketChannel =
            new SyncPacketChannel("unknown", server.identifier, 6, tempChannelsHandler)

        def deflect(): Unit = packetReader.nextPacket {
            case (concerned: Packet, coords: PacketCoordinates) if coords.channelID == channel.channelID => channel.injectPacket(concerned)
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
