package fr.overridescala.linkkit.server.connection

import java.net.Socket

import fr.overridescala.linkkit.api.exceptions.{RelayException, RelayInitialisationException, UnexpectedPacketException}
import fr.overridescala.linkkit.api.packet._
import fr.overridescala.linkkit.api.packet.channel.{AsyncPacketChannel, PacketChannel, SyncPacketChannel}
import fr.overridescala.linkkit.api.packet.fundamental._
import fr.overridescala.linkkit.api.system._
import fr.overridescala.linkkit.api.task.TasksHandler
import fr.overridescala.linkkit.server.RelayServer
import fr.overridescala.linkkit.server.task.ConnectionTasksHandler
import org.jetbrains.annotations.Nullable

import scala.util.control.NonFatal

class ClientConnection private(socket: SocketContainer,
                               server: RelayServer,
                               val identifier: String) extends JustifiedCloseable {

    private val connectionThread = new Thread(() => run(), s"RP Connection($identifier)")

    private val packetManager = server.packetManager
    private val notifier = server.eventObserver.notifier
    private val manager: ConnectionsManager = server.connectionsManager

    private val connectionTraffic = new SimpleTrafficHandler(server, socket)
    private val serverTraffic = server.trafficHandler

    private val configuration = server.configuration
    val systemChannel: SystemPacketChannel = new SystemPacketChannel(identifier, connectionTraffic)

    /**
     * null if task handling is disabled according to configurations
     * */
    @Nullable @volatile private var tasksHandler: TasksHandler = _
    @volatile private var closed = false

    private var remoteConsoleErr: RemoteConsole.Err = _
    private var remoteConsoleOut: RemoteConsole = _

    override def close(reason: Reason): Unit = {
        val threadName = connectionThread.getName
        println(s"Closing thread '$threadName'")

        if (socket.isConnected && reason.isInternal) {
            systemChannel.sendOrder(SystemOrder.CLIENT_CLOSE, reason)
        }

        tasksHandler.close(reason)
        socket.close(reason)
        connectionTraffic.close(reason)
        manager.unregister(identifier)

        closed = true
    }

    def start(): Unit = {
        if (closed)
            throw new RelayException("This Connection was already used and is now definitely closed.")
        connectionThread.start()
    }

    def isConnected: Boolean = socket.isConnected

    def getTasksHandler: TasksHandler = tasksHandler

    def getConsoleOut: RemoteConsole = remoteConsoleOut

    def getConsoleErr: RemoteConsole.Err = remoteConsoleErr

    def createSync(id: Int, cacheSize: Int): PacketChannel.Sync =
        new SyncPacketChannel(identifier, id, cacheSize, connectionTraffic)

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

    private def load(): Unit = {

        val outOpt = server.getConsoleOut(identifier)
        val errOpt = server.getConsoleErr(identifier)
        if (outOpt.isEmpty || errOpt.isEmpty)
            throw RelayInitialisationException(s"Could not retrieve remote consoles of relay '$identifier'")

        remoteConsoleErr = errOpt.get
        remoteConsoleOut = outOpt.get
        systemChannel.sendOrder(SystemOrder.PRINT_VERSION, Reason.INTERNAL)

        if (configuration.enableTasks)
            tasksHandler = new ConnectionTasksHandler(identifier, server, systemChannel, remoteConsoleErr)
    }

    private def run(): Unit = {

        val threadName = connectionThread.getName
        load()
        println(s"Thread '$threadName' was started")

        try {
            val packetReader = new ServerPacketReader(socket, server, identifier)
            while (!closed)
                packetReader.nextPacket(handlePacket)
        } catch {
            case NonFatal(e) =>
                remoteConsoleErr.reportException(e)
                e.printStackTrace()
                close(Reason.INTERNAL_ERROR)
        }
        println(s"End of Thread execution '$threadName'")
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

        import SystemOrder._
        orderType match {
            case CLIENT_CLOSE => close(reason)
            case SERVER_CLOSE => server.close(identifier, reason)
            case ABORT_TASK => tasksHandler.skipCurrent(reason)
            case CHECK_ID => checkIDRegistered(new String(content))
            case PRINT_VERSION => server.getConsoleOut(identifier).get.println(s"Connected to server ${server.relayVersion} (${server.apiVersion})")

            case _ => systemChannel.sendPacket(ErrorPacket("forbidden order", s"could not complete order '$orderType', can't be handled by a server or unknown order"))
        }
    }

    private def checkIDRegistered(target: String): Unit = {
        val response = if (manager.containsIdentifier(target)) "OK" else "ERROR"
        systemChannel.sendPacket(DataPacket(response))
    }

}

object ClientConnection {

    def open(socket: SocketContainer, server: RelayServer, identifier: String = null): ClientConnection = {
        val relayIdentifier = if (identifier == null) retrieveIdentifier(socket, server) else identifier
        val connection = new ClientConnection(socket, server, relayIdentifier)
        connection.start()
        connection
    }

    def retrieveIdentifier(socket: SocketContainer, server: RelayServer): String = {
        val packetReader = new ServerPacketReader(socket, server, null)
        val tempHandler = new SimpleTrafficHandler(server, socket)

        val channel: SyncPacketChannel =
            new SyncPacketChannel("unknown", 6, 8, tempHandler)

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
