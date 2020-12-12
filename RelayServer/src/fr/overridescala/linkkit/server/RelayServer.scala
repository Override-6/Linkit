package fr.overridescala.linkkit.server

import java.net.{ServerSocket, Socket, SocketException}
import java.nio.charset.Charset
import java.nio.file.{Path, Paths}

import fr.overridescala.linkkit.api.Relay
import fr.overridescala.linkkit.api.`extension`.{RelayExtensionLoader, RelayProperties}
import fr.overridescala.linkkit.api.exception.RelayCloseException
import fr.overridescala.linkkit.api.packet._
import fr.overridescala.linkkit.api.packet.channel.PacketChannel
import fr.overridescala.linkkit.api.packet.collector.{AsyncPacketCollector, PacketCollector, SyncPacketCollector}
import fr.overridescala.linkkit.api.packet.fundamental.DataPacket
import fr.overridescala.linkkit.api.system._
import fr.overridescala.linkkit.api.system.event.EventObserver
import fr.overridescala.linkkit.api.system.security.{RelaySecurityException, RelaySecurityManager}
import fr.overridescala.linkkit.api.task.{Task, TaskCompleterHandler}
import fr.overridescala.linkkit.server.RelayServer.Identifier
import fr.overridescala.linkkit.server.config.{AmbiguityStrategy, RelayServerConfiguration}
import fr.overridescala.linkkit.server.connection.{ClientConnection, ConnectionsManager, SocketContainer}
import fr.overridescala.linkkit.server.security.RelayServerSecurityManager

import scala.util.control.NonFatal

object RelayServer {
    val version: Version = Version("RelayServer", "0.8.0", stable = false)

    val Identifier = "server"
}

class RelayServer(override val configuration: RelayServerConfiguration) extends Relay {

    private val serverSocket = new ServerSocket(configuration.port)
    private val taskFolderPath = getTasksFolderPath

    @volatile private var open = false
    /**
     * For safety, prefer Relay#identfier instead of Constants.SERVER_ID
     * */
    override val identifier: String = Identifier
    override val eventObserver: EventObserver = new EventObserver(configuration.enableEventHandling)
    override val extensionLoader = new RelayExtensionLoader(this)
    override val taskCompleterHandler = new TaskCompleterHandler
    override val properties: RelayProperties = new RelayProperties
    override val packetManager = new PacketManager(this)
    override val securityManager: RelayServerSecurityManager = configuration.securityManager

    override val relayVersion: Version = RelayServer.version

    private[server] val notifier = eventObserver.notifier

    val trafficHandler = new ServerTrafficHandler(this)
    val connectionsManager = new ConnectionsManager(this)

    private val remoteConsoles: RemoteConsolesHandler = new RemoteConsolesHandler(this)

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
        println(apiVersion)

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
                close(Reason.INTERNAL_ERROR)
        }

        println("Ready !")
        notifier.onReady()
    }


    override def createSyncChannel(linkedRelayID: String, id: Int, cacheSize: Int): PacketChannel.Sync = {
        val targetConnection = getConnection(linkedRelayID)
        targetConnection.createSync(id, cacheSize)
    }


    override def createAsyncChannel(linkedRelayID: String, id: Int): PacketChannel.Async = {
        val targetConnection = getConnection(linkedRelayID)
        targetConnection.createAsync(id)
    }

    override def createSyncCollector(id: Int, cacheSize: Int): PacketCollector.Sync = {
        new SyncPacketCollector(trafficHandler, cacheSize, id)
    }

    override def createAsyncCollector(id: Int): PacketCollector.Async = {
        new AsyncPacketCollector(trafficHandler, id)
    }

    override def getConsoleOut(targetId: String): Option[RemoteConsole] = {
        if (connectionsManager.isNotRegistered(targetId))
            return Option.empty

        Option(remoteConsoles.getOut(targetId))
    }

    override def getConsoleErr(targetId: String): Option[RemoteConsole.Err] = {
        if (connectionsManager.isNotRegistered(targetId))
            return Option.empty

        Option(remoteConsoles.getErr(targetId))
    }

    override def close(reason: Reason): Unit =
        close(identifier, reason)


    def close(relayId: String, reason: Reason): Unit = {
        println("closing server...")

        if (reason == Reason.INTERNAL_ERROR)
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

    private def registerConnection(identifier: String, socket: Socket): Unit = {
        val socketContainer = new SocketContainer(notifier, true)
        socketContainer.set(socket)
        connectionsManager.register(socketContainer, identifier)
    }

    private def handleRelayPointConnection(identifier: String, tempSocket: SocketContainer): Unit = {

        if (connectionsManager.isNotRegistered(identifier)) {
            registerConnection(identifier, tempSocket.get)
            sendResponse(tempSocket, "OK")
            return
        }

        handleConnectionIDAmbiguity(getConnection(identifier), tempSocket)
    }

    private def handleConnectionIDAmbiguity(current: ClientConnection, tempSocket: SocketContainer): Unit = {

        if (!current.isConnected) {
            current.updateSocket(tempSocket.get)
            sendResponse(tempSocket, "OK")
            return
        }
        val identifier = current.identifier
        val rejectMsg = s"Another relay point with id '$identifier' is currently connected on the targeted network."

        import AmbiguityStrategy._
        configuration.relayIDAmbiguityStrategy match {
            case CLOSE_SERVER =>
                sendResponse(tempSocket, "ERROR", rejectMsg + " Consequences: Closing Server...")
                broadcast(true, "RelayServer will close your connection because of a critical error")
                close(Reason.INTERNAL_ERROR)

            case REJECT_NEW =>
                Console.err.println("Rejected connection of a client because he gave an already registered relay identifier.")
                sendResponse(tempSocket, "ERROR", rejectMsg)

            case REPLACE =>
                connectionsManager.unregister(identifier).close(Reason.INTERNAL_ERROR)
                registerConnection(identifier, tempSocket.get)
                sendResponse(tempSocket, "OK")

            case DISCONNECT_BOTH =>
                connectionsManager.unregister(identifier).close(Reason.INTERNAL_ERROR)
                sendResponse(tempSocket, "ERROR", rejectMsg + " Consequences : Disconnected both")
        }
    }

    private def listenSocketConnection(): Unit = {
        val tempSocket = new SocketContainer(notifier, false)
        try {
            val clientSocket = serverSocket.accept()
            tempSocket.set(clientSocket)

            val identifier = ClientConnection.retrieveIdentifier(tempSocket, this)
            handleRelayPointConnection(identifier, tempSocket)
        } catch {
            case e: SocketException =>
                val msg = e.getMessage.toLowerCase
                if (msg == "socket closed" || msg == "socket is closed")
                    return
                println("waaaiii")
                Console.err.println(msg)
            case e: RelayCloseException =>
            case NonFatal(e) =>
                e.printStackTrace()
                onException(e)
        }

        def onException(e: Throwable): Unit = {
            sendResponse(tempSocket, "ERROR", s"An exception occurred in server during client connection initialisation ($e)") //send a negative response for the client initialisation handling
            close(Reason.INTERNAL_ERROR)
        }
    }

    private def getTasksFolderPath: Path = {
        val path = System.getenv().get("COMPUTERNAME") match {
            case "PC_MATERIEL_NET" => "C:\\Users\\maxim\\Desktop\\Dev\\VPS\\ClientSide\\RelayExtensions"
            case _ => configuration.extensionsFolder
        }
        Paths.get(path)
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

    Runtime.getRuntime.addShutdownHook(new Thread(() => close(Reason.INTERNAL)))
}