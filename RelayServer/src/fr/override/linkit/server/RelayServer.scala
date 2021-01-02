package fr.`override`.linkit.server

import java.net.{ServerSocket, SocketException}
import java.nio.charset.Charset

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.`extension`.{RelayExtensionLoader, RelayProperties}
import fr.`override`.linkit.api.exception.RelayCloseException
import fr.`override`.linkit.api.network.ConnectionState
import fr.`override`.linkit.api.packet._
import fr.`override`.linkit.api.packet.channel.PacketChannel
import fr.`override`.linkit.api.packet.collector.{AsyncPacketCollector, PacketCollector, SyncPacketCollector}
import fr.`override`.linkit.api.packet.fundamental.DataPacket
import fr.`override`.linkit.api.system._
import fr.`override`.linkit.api.system.event.EventObserver
import fr.`override`.linkit.api.task.{Task, TaskCompleterHandler}
import fr.`override`.linkit.server.RelayServer.Identifier
import fr.`override`.linkit.server.config.{AmbiguityStrategy, RelayServerConfiguration}
import fr.`override`.linkit.server.connection.{ClientConnection, ConnectionsManager, SocketContainer}
import fr.`override`.linkit.server.exceptions.ConnectionInitialisationException
import fr.`override`.linkit.server.network.ServerNetwork
import fr.`override`.linkit.server.security.RelayServerSecurityManager

import scala.util.control.NonFatal

object RelayServer {
    val version: Version = Version("RelayServer", "0.13.0", stable = false)

    val Identifier = "server"
}

//TODO Create a connection helper for this poor class which swims into bad practices.
class RelayServer private[server](override val configuration: RelayServerConfiguration) extends Relay {

    private val serverSocket = new ServerSocket(configuration.port)

    @volatile private var open = false

    private[server] val connectionsManager = new ConnectionsManager(this)

    override val trafficHandler = new ServerTrafficHandler(this) // FIXME I Think this is very bad to have this like this
    override val identifier: String = Identifier
    override val eventObserver: EventObserver = new EventObserver(configuration.enableEventHandling)
    override val extensionLoader = new RelayExtensionLoader(this)
    override val taskCompleterHandler = new TaskCompleterHandler
    override val properties: RelayProperties = new RelayProperties
    override val packetManager = new PacketManager(this)
    override val securityManager: RelayServerSecurityManager = configuration.securityManager
    override val network: ServerNetwork = new ServerNetwork(this)

    override val relayVersion: Version = RelayServer.version

    private[server] val notifier = eventObserver.notifier

    private val remoteConsoles: RemoteConsolesContainer = new RemoteConsolesContainer(this)

    override def scheduleTask[R](task: Task[R]): RelayTaskAction[R] = {
        ensureOpen()
        val targetIdentifier = task.targetID
        val connection = getConnection(targetIdentifier)
        if (connection == null)
            throw new NoSuchElementException(s"Unknown or unregistered relay with identifier '$targetIdentifier'")

        val tasksHandler = connection.getTasksHandler
        task.preInit(tasksHandler)
        notifier.onTaskScheduled(task)
        RelayTaskAction.of(task)
    }

    override def start(): Unit = {
        println("Current encoding is " + Charset.defaultCharset().name())
        println("Listening on port " + configuration.port)
        println("Computer name is " + System.getenv().get("COMPUTERNAME"))
        println("Relay Identifier Ambiguity Strategy : " + configuration.relayIDAmbiguityStrategy)
        println(relayVersion)
        println(Relay.ApiVersion)

        try {
            securityManager.checkRelay(this)

            if (configuration.enableExtensionsFolderLoad)
                extensionLoader.loadExtensions()

            val thread = new Thread(() => {
                open = true
                while (open) listenSocketConnection()
            })
            thread.setName("Socket Connection Listener")
            thread.start()

            securityManager.checkRelay(this)
        } catch {
            case NonFatal(e) =>
                e.printStackTrace()
                close(CloseReason.INTERNAL_ERROR)
        }

        println("Ready !")
        notifier.onReady()
    }

    override def addConnectionListener(action: ConnectionState => Unit): Unit = () //the connection of the server would never be updated

    override def getState: ConnectionState = ConnectionState.CONNECTED //The server is always connected to itself !

    override def isConnected(identifier: String): Boolean = {
        connectionsManager.containsIdentifier(identifier)
    }

    override def createSyncChannel(linkedRelayID: String, id: Int, cacheSize: Int): PacketChannel.Sync = {
        getConnection(linkedRelayID).createSync(id, cacheSize)
    }


    override def createAsyncChannel(linkedRelayID: String, id: Int): PacketChannel.Async = {
        getConnection(linkedRelayID).createAsync(id)
    }

    override def createSyncCollector(id: Int, cacheSize: Int): PacketCollector.Sync = {
        new SyncPacketCollector(trafficHandler, cacheSize, id)
    }

    override def createAsyncCollector(id: Int): PacketCollector.Async = {
        new AsyncPacketCollector(trafficHandler, id)
    }

    override def getConsoleOut(targetId: String): RemoteConsole = {
        remoteConsoles.getOut(targetId)
    }

    override def getConsoleErr(targetId: String): RemoteConsole = {
        remoteConsoles.getErr(targetId)
    }

    override def close(reason: CloseReason): Unit =
        close(identifier, reason)


    def close(relayId: String, reason: CloseReason): Unit = {
        println("closing server...")

        if (reason == CloseReason.INTERNAL_ERROR)
            broadcast(true, "RelayServer will close your connection because of a critical error")

        extensionLoader.close()
        connectionsManager.close(reason)
        serverSocket.close()

        open = false
        notifier.onClosed(relayId, reason)
        println("server closed !")
    }

    def getConnection(relayIdentifier: String): ClientConnection = {
        ensureOpen()
        connectionsManager.getConnection(relayIdentifier)
    }

    def broadcast(err: Boolean, msg: String): Unit = {
        connectionsManager.broadcast(err, "(broadcast) " + msg)
    }

    private def handleRelayPointConnection(identifier: String,
                                           socket: SocketContainer): Unit = {

        if (connectionsManager.isNotRegistered(identifier)) {
            connectionsManager.registerConnection(identifier, socket)
            sendResponse(socket, "OK")
            return
        }

        handleConnectionIDAmbiguity(getConnection(identifier), socket)
    }

    private def handleConnectionIDAmbiguity(current: ClientConnection,
                                            socket: SocketContainer): Unit = {

        if (!current.isConnected) {
            current.updateSocket(socket.get)
            sendResponse(socket, "OK")
            return
        }
        val identifier = current.identifier
        val rejectMsg = s"Another relay point with id '$identifier' is currently connected on the targeted network."

        import AmbiguityStrategy._
        configuration.relayIDAmbiguityStrategy match {
            case CLOSE_SERVER =>
                sendResponse(socket, "ERROR", rejectMsg + " Consequences: Closing Server...")
                broadcast(true, "RelayServer will close your connection because of a critical error")
                close(CloseReason.INTERNAL_ERROR)

            case REJECT_NEW =>
                Console.err.println("Rejected connection of a client because he gave an already registered relay identifier.")
                sendResponse(socket, "ERROR", rejectMsg)

            case REPLACE =>
                connectionsManager.unregister(identifier).get.close(CloseReason.INTERNAL_ERROR)
                connectionsManager.registerConnection(identifier, socket)
                sendResponse(socket, "OK")

            case DISCONNECT_BOTH =>
                connectionsManager.unregister(identifier).get.close(CloseReason.INTERNAL_ERROR)
                sendResponse(socket, "ERROR", rejectMsg + " Consequences : Disconnected both")
        }
    }

    private def listenSocketConnection(): Unit = {
        val socketContainer = new SocketContainer(notifier, true)
        try {
            val clientSocket = serverSocket.accept()
            socketContainer.set(clientSocket)

            val welcomePacketLength = socketContainer.read(1).head
            if (welcomePacketLength > 32)
                throw new ConnectionInitialisationException("Relay identifier exceeded maximum size limit of 32")
            val welcomePacket = socketContainer.read(welcomePacketLength)


            val identifier = new String(welcomePacket)
            handleRelayPointConnection(identifier, socketContainer)
        } catch {
            case e: SocketException =>
                val msg = e.getMessage.toLowerCase
                if (msg == "socket closed" || msg == "socket is closed")
                    return
                Console.err.println(msg)
                onException(e)
            case e: RelayCloseException => onException(e)
            case NonFatal(e) =>
                e.printStackTrace()
                onException(e)
        }

        def onException(e: Throwable): Unit = {
            sendResponse(socketContainer, "ERROR", s"An exception occurred in server during client connection initialisation ($e)") //sends a negative response for the client initialisation handling
            close(CloseReason.INTERNAL_ERROR)
        }
    }

    private def ensureOpen(): Unit = {
        if (!open)
            throw new RelayCloseException("Relay Server have to be started !")
    }

    private def sendResponse(socket: DynamicSocket, response: String, message: String = ""): Unit = {
        val responsePacket = DataPacket(response, message)
        val coordinates = PacketCoordinates(SystemPacketChannel.SystemChannelID, "unknown", identifier)
        socket.write(packetManager.toBytes(responsePacket, coordinates))
    }

    Runtime.getRuntime.addShutdownHook(new Thread(() => close(CloseReason.INTERNAL)))

}