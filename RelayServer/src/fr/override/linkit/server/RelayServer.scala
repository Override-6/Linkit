package fr.`override`.linkit.server

import java.net.{ServerSocket, SocketException}
import java.nio.charset.Charset

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.`extension`.{RelayExtensionLoader, RelayProperties}
import fr.`override`.linkit.api.exception.RelayCloseException
import fr.`override`.linkit.api.network.ConnectionState
import fr.`override`.linkit.api.packet._
import fr.`override`.linkit.api.packet.channel.{PacketChannel, PacketChannelFactory}
import fr.`override`.linkit.api.packet.collector.{PacketCollector, PacketCollectorFactory}
import fr.`override`.linkit.api.packet.fundamental.DataPacket
import fr.`override`.linkit.api.packet.traffic.DynamicSocket
import fr.`override`.linkit.api.system._
import fr.`override`.linkit.api.task.{Task, TaskCompleterHandler}
import fr.`override`.linkit.server.RelayServer.Identifier
import fr.`override`.linkit.server.config.{AmbiguityStrategy, RelayServerConfiguration}
import fr.`override`.linkit.server.connection.{ClientConnection, ConnectionsManager, SocketContainer}
import fr.`override`.linkit.server.exceptions.ConnectionInitialisationException
import fr.`override`.linkit.server.network.ServerNetwork
import fr.`override`.linkit.server.security.RelayServerSecurityManager

import scala.util.control.NonFatal

object RelayServer {
    val version: Version = Version("RelayServer", "0.14.0", stable = false)

    val Identifier = "server"
}

//TODO Create a connection helper for this poor class which swims into bad practices.
class RelayServer private[server](override val configuration: RelayServerConfiguration) extends Relay {

    private val serverSocket = new ServerSocket(configuration.port)
    private val globalTraffic = new GlobalPacketTraffic(this)
    private val remoteConsoles: RemoteConsolesContainer = new RemoteConsolesContainer(this)

    @volatile private var open = false

    private[server] val connectionsManager = new ConnectionsManager(this)

    override val identifier: String = Identifier
    override val extensionLoader = new RelayExtensionLoader(this)
    override val taskCompleterHandler = new TaskCompleterHandler
    override val properties: RelayProperties = new RelayProperties
    override val packetTranslator = new PacketTranslator(this)
    override val securityManager: RelayServerSecurityManager = configuration.securityManager
    override val network: ServerNetwork = new ServerNetwork(this)

    override val relayVersion: Version = RelayServer.version


    override def scheduleTask[R](task: Task[R]): RelayTaskAction[R] = {
        ensureOpen()
        val targetIdentifier = task.targetID
        val connection = getConnection(targetIdentifier)
        if (connection == null)
            throw new NoSuchElementException(s"Unknown or unregistered relay with identifier '$targetIdentifier'")

        val tasksHandler = connection.getTasksHandler
        task.preInit(tasksHandler)
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
    }

    override def addConnectionListener(action: ConnectionState => Unit): Unit = () //the connection of the server would never be updated

    override def getState: ConnectionState = ConnectionState.CONNECTED //The server is always connected to itself !

    override def isConnected(identifier: String): Boolean = {
        connectionsManager.containsIdentifier(identifier)
    }

    override def createChannel[C <: PacketChannel](channelId: Int, targetID: String, factory: PacketChannelFactory[C]): C = {
        val channel = factory.createNew(globalTraffic, channelId, targetID)
        globalTraffic.register(channel)
        channel
    }

    override def createCollector[C <: PacketCollector](channelId: Int, factory: PacketCollectorFactory[C]): C = {
        val channel = factory.createNew(globalTraffic, channelId)
        globalTraffic.register(channel)
        channel
    }

    override def getConsoleOut(targetId: String): RemoteConsole = {
        remoteConsoles.getOut(targetId)
    }

    override def getConsoleErr(targetId: String): RemoteConsole = {
        remoteConsoles.getErr(targetId)
    }

    override def close(reason: CloseReason): Unit = {
        println("closing server...")
        Thread.dumpStack()

        if (reason == CloseReason.INTERNAL_ERROR)
            broadcast(true, "RelayServer will close your connection because of a critical error")

        extensionLoader.close()
        connectionsManager.close(reason)
        serverSocket.close()

        open = false
        println("server closed !")
    }

    override def isClosed: Boolean = !open

    def getConnection(relayIdentifier: String): ClientConnection = {
        ensureOpen()
        connectionsManager.getConnection(relayIdentifier)
    }

    def broadcast(err: Boolean, msg: String): Unit = {
        connectionsManager.broadcast(err, "(broadcast) " + msg)
    }

    /**
     * Pre handles a packet, if this packet is a global one, it will be injected into the global traffic.
     *
     * Any packet received by a [[ClientConnection]] will invoke this method before it could be handled by the connection.
     * If the handled packet prove to be a global packet,
     * the client connection that just received this packet will stop handling it.
     * (that's why this method is named as 'preHandlePacket' !)
     *
     * @param packet the packet to inject whether his coordinates prove to be a global packet
     * @param coordinates the packet coordinates to check. If the injectableID is registered in the global traffic,
     *                    that's means the packet is global.
     * @return true if the following handling int the client connection should stop, false instead
     * */
    private[server] def preHandlePacket(packet: Packet, coordinates: PacketCoordinates): Boolean = {
        val isGlobalPacket = globalTraffic.isRegistered(coordinates.injectableID)
        if (isGlobalPacket)
            globalTraffic.injectPacket(packet, coordinates)
        !isGlobalPacket
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
        val socketContainer = new SocketContainer(true)
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
        socket.write(packetTranslator.toBytes(responsePacket, coordinates))
    }

    Runtime.getRuntime.addShutdownHook(new Thread(() => close(CloseReason.INTERNAL)))

}