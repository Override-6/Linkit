package fr.`override`.linkit.server.connection

import java.net.Socket

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.server.RelayServer
import fr.`override`.linkit.server.task.ConnectionTasksHandler
import fr.`override`.linkit.api.exception.{RelayException, RelayInitialisationException, UnexpectedPacketException}
import fr.`override`.linkit.api.packet._
import fr.`override`.linkit.api.packet.channel.{AsyncPacketChannel, PacketChannel, SyncPacketChannel}
import fr.`override`.linkit.api.packet.fundamental._
import fr.`override`.linkit.api.system._
import fr.`override`.linkit.api.system.network.ConnectionState
import fr.`override`.linkit.api.task.TasksHandler
import org.jetbrains.annotations.{NotNull, Nullable}

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class ClientConnection private(socket: SocketContainer,
                               server: RelayServer,
                               val identifier: String) extends JustifiedCloseable {

    private val connectionThread = new Thread(server.packetWorkerThreadGroup, () => run(), s"Dedicated Packet Worker ($identifier)")

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
    @Nullable
    @volatile private var tasksHandler: TasksHandler = _
    @volatile private var closed = false

    private var remoteConsoleErr: RemoteConsole.Err = _
    private var remoteConsoleOut: RemoteConsole = _

    override def close(reason: CloseReason): Unit = {
        if (reason.isInternal && isConnected) {
            systemChannel.sendOrder(SystemOrder.CLIENT_CLOSE, reason)
        }

        if (tasksHandler != null)
            tasksHandler.close(reason)
        socket.close(reason)
        connectionTraffic.close(reason)
        manager.unregister(identifier)
        connectionThread.interrupt()

        closed = true
    }

    def start(cachePacket: Seq[(Packet, PacketCoordinates)]): Unit = {
        if (closed)
            throw new RelayException("This Connection was already used and is now definitely closed.")
        connectionThread.start()
        loadRemote()
        cachePacket.foreach(pair => handlePacket(pair._1, pair._2))
    }

    def isConnected: Boolean = getState == ConnectionState.CONNECTED

    def getTasksHandler: TasksHandler = tasksHandler

    def getConsoleOut: RemoteConsole = remoteConsoleOut

    def getConsoleErr: RemoteConsole.Err = remoteConsoleErr

    def getState: ConnectionState = socket.getState

    def addConnectionStateListener(action: ConnectionState => Unit): Unit = socket.addConnectionStateListener(action)

    def createSync(id: Int, cacheSize: Int): PacketChannel.Sync =
        new SyncPacketChannel(identifier, id, cacheSize, connectionTraffic)

    def createAsync(id: Int): PacketChannel.Async = {
        new AsyncPacketChannel(identifier, id, connectionTraffic)
    }

    def sendPacket(packet: Packet, channelID: Int): Unit = {
        val bytes = packetManager.toBytes(packet, PacketCoordinates(channelID, identifier, server.identifier))
        socket.write(bytes)
    }

    private[server] def updateSocket(socket: Socket): Unit =
        this.socket.set(socket)

    private[connection] def sendDeflectedBytes(bytes: Array[Byte]): Unit = {
        socket.write(PacketUtils.wrap(bytes))
    }

    private def loadRemote(): Unit = {
        systemChannel.sendPacket(DataPacket("OK"))

        val outOpt = server.getConsoleOut(identifier)
        val errOpt = server.getConsoleErr(identifier)
        if (outOpt.isEmpty || errOpt.isEmpty)
            throw RelayInitialisationException(s"Could not retrieve remote consoles of relay '$identifier'")

        remoteConsoleErr = errOpt.get
        remoteConsoleOut = outOpt.get
        systemChannel.sendOrder(SystemOrder.PRINT_INFO, CloseReason.INTERNAL)

        if (configuration.enableTasks)
            tasksHandler = new ConnectionTasksHandler(server, systemChannel, this)
    }

    private def run(): Unit = {
        val threadName = connectionThread.getName
        println(s"Thread '$threadName' was started")

        try {
            val packetReader = new ServerPacketReader(socket, server, identifier)
            while (!closed)
                packetReader.nextPacket(handlePacket)
        } catch {
            case NonFatal(e) =>
                e.printStackTrace()
                remoteConsoleErr.reportException(e)
                close(CloseReason.INTERNAL_ERROR)
        }
        println(s"End of Thread execution '$threadName'")
    }

    private def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        val identifier = coordinates.containerID
        if (closed)
            return
        notifier.onPacketReceived(packet, coordinates)
        packet match {
            case systemError: ErrorPacket if identifier == systemChannel.identifier => systemError.printError()
            case systemPacket: SystemPacket => handleSystemOrder(systemPacket)
            case init: TaskInitPacket => tasksHandler.handlePacket(init, coordinates)
            case _: Packet =>
                if (serverTraffic.isRegistered(coordinates.containerID)) {
                    serverTraffic.injectPacket(packet, coordinates)
                    return
                }
                connectionTraffic.injectPacket(packet, coordinates)
        }
    }


    private def handleSystemOrder(packet: SystemPacket): Unit = {
        val orderType = packet.order
        val reason = packet.reason.reversedPOV()
        val content = packet.content

        notifier.onSystemOrderReceived(orderType, reason)
        import SystemOrder._
        orderType match {
            case CLIENT_CLOSE => close(reason)
            case SERVER_CLOSE => server.close(identifier, reason)
            case ABORT_TASK => tasksHandler.skipCurrent(reason)
            case CHECK_ID => checkIDRegistered(new String(content))
            case PRINT_INFO => server.getConsoleOut(identifier).get.println(s"Connected to server ${server.relayVersion} (${Relay.ApiVersion})")

            case _ => systemChannel.sendPacket(ErrorPacket("Forbidden order", s"Could not complete order '$orderType', can't be handled by a server or unknown order"))
        }
    }

    private def checkIDRegistered(target: String): Unit = {
        val response = if (server.isConnected(target)) "OK" else "ERROR"
        systemChannel.sendPacket(DataPacket(response))
    }

}

object ClientConnection {

    /**
     * Constructs a ClientConnection without starting it.
     * @param socket the socket which will be used by the connection to perform in/out data transfer
     * @param server the server which is asking for this client connection
     * @param identifier The identifier of the connected relay.
     * @throws NullPointerException if the identifier or the socket is null.
     * @return a non-started ClientConnection.
     * @see [[SocketContainer]]
     * */
    def preOpen(@NotNull socket: SocketContainer,
                @NotNull server: RelayServer,
                @NotNull identifier: String): ClientConnection = {
        if (socket == null || identifier == null || server == null)
            throw new NullPointerException("Unable to construct ClientConnection : one of the given parameters are null")
        new ClientConnection(socket, server, identifier)
    }

    /**
     * Helper method which retrieves an identifier by writing the system order "GET_IDENTIFIER" on the socket
     * @param socket The socket in which the request will be write and received
     * @param server The server which is asking for this request
     * @param unhandledPackets a list of any packet that has been received, but could not be handled because they haven't concern the authentication.
     * @return
     * */
    def retrieveIdentifier(socket: SocketContainer,
                           server: RelayServer,
                           unhandledPackets: ListBuffer[(Packet, PacketCoordinates)]): String = {
        val packetReader = new ServerPacketReader(socket, server, null)
        val tempHandler = new SimpleTrafficHandler(server, socket)

        val channel: SyncPacketChannel =
            new SyncPacketChannel("unknown", 6, 8, tempHandler)

        def deflect(): Unit = packetReader.nextPacket {
            case (concerned: Packet, coords: PacketCoordinates) if coords.containerID == channel.identifier =>
                channel.injectPacket(concerned, coords)
            case other =>
                unhandledPackets += other
                deflect()
        }

        def handleClientResponse(): String = channel.nextPacket() match {
            case dataPacket: DataPacket => dataPacket.header
            case other =>
                val name = other.getClass.getSimpleName
                throw new UnexpectedPacketException(s"Unexpected packet type $name received while getting RelayPoint identifier.")
        }

        channel.sendPacket(SystemPacket(SystemOrder.GET_IDENTIFIER, CloseReason.INTERNAL))
        deflect()

        val identifier = handleClientResponse()
        channel.close(CloseReason.INTERNAL)

        identifier
    }

}
