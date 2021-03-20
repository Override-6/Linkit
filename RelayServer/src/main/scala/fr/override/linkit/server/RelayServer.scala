package fr.`override`.linkit.server

import fr.`override`.linkit.api.connection.network._
import fr.`override`.linkit.api.connection.packet._
import fr.`override`.linkit.api.connection.packet.traffic.ChannelScope.ScopeFactory
import fr.`override`.linkit.api.connection.packet.traffic._
import fr.`override`.linkit.api.connection.task.Task
import fr.`override`.linkit.api.local.system.RelayState.{CLOSED, CRASHED, ENABLED, ENABLING}
import fr.`override`.linkit.api.local.system._
import fr.`override`.linkit.core.connection.packet.fundamental.RefPacket.StringPacket
import fr.`override`.linkit.core.connection.packet.fundamental.ValPacket.BooleanPacket
import fr.`override`.linkit.server.config.{AmbiguityStrategy, ServerConnectionConfiguration}
import fr.`override`.linkit.server.connection.{ConnectionInitialisationException, ConnectionsManager, ServerExternalConnection, SocketContainer}
import fr.`override`.linkit.server.network.ServerNetwork
import fr.`override`.linkit.server.security.ServerSecurityManager

import java.net.{ServerSocket, Socket, SocketException}
import java.nio.charset.Charset
import scala.reflect.ClassTag
import scala.util.control.NonFatal

object RelayServer {
    val version: Version = Version("RelayServer", "0.18.0", stable = false)

}


class RelayServer private[server](override val configuration: ServerConnectionConfiguration) extends Relay {

    override val identifier: String = Identifier

    private val serverSocket = new ServerSocket(configuration.port)

    @volatile private var open = false
    private[server] val connectionsManager = new ConnectionsManager(this)

    private var currentState: RelayState = RelayState.INACTIVE
    private val workerThread: BusyWorkerPool = new BusyWorkerPool("\b", 3)
    override val eventNotifier: EventNotifier = new EventNotifier
    override val securityManager: ServerSecurityManager = configuration.securityManager
    override val traffic: PacketTraffic = new ServerPacketTraffic(this)
    override val relayVersion: Version = RelayServer.version
    override val extensionLoader: RelayExtensionLoader = new RelayExtensionLoader(this)
    override val taskCompleterHandler: TaskCompleterHandler = new TaskCompleterHandler
    override val properties: RelayProperties = new RelayProperties
    override val packetTranslator: PacketTranslator = new PacketTranslator(this)
    override val network: ServerNetwork = new ServerNetwork(this, traffic)
    private val remoteConsoles: RemoteConsolesContainer = new RemoteConsolesContainer(this)

    override def scheduleTask[R](task: Task[R]): RelayTaskAction[R] = {
        ensureOpen()
        val targetIdentifier = task.targetID
        val connection = getConnection(targetIdentifier)
        if (connection.isEmpty)
            throw new NoSuchElementException(s"Unknown or unregistered relay with identifier '$targetIdentifier'")

        val tasksHandler = connection.get.getTasksHandler
        task.preInit(tasksHandler)
        RelayTaskAction.of(task)
    }


    override def start(): Unit = {
        BusyWorkerPool.checkCurrentIsWorker("Must start server in a worker thread.")
        setState(ENABLING)

        Log.info("Current encoding is " + Charset.defaultCharset().name())
        Log.info("Listening on port " + configuration.port)
        Log.info("Computer name is " + System.getenv().get("COMPUTERNAME"))
        Log.info("Relay Identifier Ambiguity Strategy : " + configuration.relayIDAmbiguityStrategy)
        Log.info(relayVersion)
        Log.info(Relay.ApiVersion)

        try {
            loadInternal()
            loadSocketListener()
        } catch {
            case NonFatal(e) =>
                e.printStackTrace()
                close(CloseReason.INTERNAL_ERROR)
                throw AppInitialisationException(e.getMessage, e)
        }
        setState(ENABLED)
        Log.info("Ready !")
    }

    override def getConnectionState: ConnectionState = ConnectionState.CONNECTED //The server is always connected to itself !

    override def getInjectable[C <: PacketInjectable : ClassTag](channelId: Int, scopeFactory: ScopeFactory[_ <: ChannelScope], factory: PacketInjectableFactory[C]): C = {
        traffic.getInjectable(channelId, scopeFactory, factory)
    }

    override def getConsoleOut(targetId: String): RemoteConsole = {
        remoteConsoles.getOut(targetId)
    }

    override def getConsoleErr(targetId: String): RemoteConsole = {
        remoteConsoles.getErr(targetId)
    }

    override def close(reason: CloseReason): Unit = {
        Log.warn("Closing server...")

        if (reason == CloseReason.INTERNAL_ERROR)
            broadcastMessage(true, "RelayServer will close your connection because of a critical error")

        extensionLoader.close()
        connectionsManager.close(reason)
        serverSocket.close()

        open = false
        if (reason == CloseReason.INTERNAL_ERROR) setState(CRASHED)
        else setState(CLOSED)

        Log.warn("Server closed !")
    }

    /**
     * Tests if the identifier is registered into the connectionsManager
     * @param identifier, a string that represent the identifier of an eventual relay connection
     * @return true if an instance of [[ServerExternalConnection]] bound to the provided identifier was found in the connectionsManager.
     * */
    override def isConnected(identifier: String): Boolean = connectionsManager.isRegistered(identifier)

    /*
    * Difference between isConnected and isFullyConnected is that this method will return true
    * if the identifier is registered into the network. if the identifier is registered in the network,
    * this means that the relay under the provided identifier is completely initialised.
    * */
    def isFullyConnected(identifier: String): Boolean = network.isConnected(identifier)

    override def runLater(callback: => Unit): this.type = {
        workerThread.runLater(callback)
        this
    }

    override def state(): RelayState = currentState

    override def isClosed: Boolean = !open


    def getConnection(relayIdentifier: String): Option[ServerExternalConnection] = {
        ensureOpen()
        Option(connectionsManager.getConnection(relayIdentifier))
    }

    def broadcastMessage(err: Boolean, msg: String): Unit = {
        connectionsManager.broadcastMessage(err, "(broadcast) " + msg)
    }

    def broadcastPacketToConnections(packet: Packet, sender: String, injectableID: Int, discarded: String*): Unit = {
        connectionsManager.broadcastBytes(packet, injectableID, sender, discarded.appended(identifier): _*)
    }

    private def ensureOpen(): Unit = {
        if (!open)
            throw new IllegalCloseException("Relay Server have to be started !")
    }

    private def setState(state: RelayState): Unit = {
        eventNotifier.notifyEvent(RelayEvents.stateChange(state))
        this.currentState = state
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////  C L I E N T  I N I T I A L I S A T I O N  H A N D L I N G  ////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //TODO Documentation

    /**
     * Reads a welcome packet from a relay.<br>
     * The Welcome packet is the first packet that a client must send in order to provide his identifier to the server.
     *
     * @return the identifier bound with the socket
     * */
    private def readWelcomePacket(socket: SocketContainer): String = {
        val welcomePacketLength = socket.readInt()
        if (welcomePacketLength > 32)
            throw new ConnectionInitialisationException("Relay identifier exceeded maximum size limit of 32")

        val welcomePacket = socket.read(welcomePacketLength)
        new String(welcomePacket)
    }

    private def listenSocketConnection(): Unit = {
        val socketContainer = new SocketContainer(true)
        try {
            println("LISTENING SOCKETS")
            val clientSocket = serverSocket.accept()
            println(s"clientSocket = ${clientSocket}")
            prepareSocket(clientSocket, socketContainer)
            println(s"socketContainer = ${socketContainer}")
            runLater {
                handleSocket(socketContainer)
                println("Done.")
            }
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
            sendRefusedConnection(socketContainer, s"An exception occurred in server during client connection initialisation ($e)") //sends a negative response for the fr.override.linkit.client initialisation handling
            close(CloseReason.INTERNAL_ERROR)
        }
    }

    private def prepareSocket(clientSocket: Socket, container: SocketContainer): Unit = {
        //Log.info(s"Accepted socket $clientSocket...")
        container.set(clientSocket)
        container.addConnectionStateListener {
            case ConnectionState.CONNECTED =>
            case ConnectionState.DISCONNECTED | ConnectionState.CLOSED => runLater {
                packetTranslator.blackListFromCachedSerializer(container.boundIdentifier)
            }
        }
    }

    private def handleSocket(socket: SocketContainer): Unit = {
        val identifier = readWelcomePacket(socket)
        socket.identifier = identifier
        handleRelayConnection(identifier, socket)
    }

    private def loadInternal(): Unit = {
        if (configuration.enableExtensionsFolderLoad)
            extensionLoader.loadMainFolder()
    }

    private def loadSocketListener(): Unit = {
        securityManager.checkRelay(this)

        val thread = new Thread(() => {
            open = true
            while (open) listenSocketConnection()
        })
        thread.setName("Socket Connection Listener")
        thread.start()

        securityManager.checkRelay(this)
    }

    private def handleRelayConnection(identifier: String,
                                      socket: SocketContainer): Unit = {

        val currentConnection = getConnection(identifier)
        //There is no currently connected connection with the same identifier on this network.
        if (currentConnection.isEmpty) {
            connectionsManager.registerConnection(identifier, socket)
            val newConnection = getConnection(identifier)

            if (newConnection.isDefined) //may be empty, in this case, the connection would be rejected.
                network.addEntity(newConnection.get)
            return
        }

        handleConnectionIdAmbiguity(currentConnection.get, socket)
    }

    private def handleConnectionIdAmbiguity(current: ServerExternalConnection,
                                            socket: SocketContainer): Unit = {

        if (!current.isConnected) {
            current.updateSocket(socket.get)
            sendAuthorisedConnection(socket)
            println(s"The connection of ${current.identifier} has been resumed.")
            return
        }

        val identifier = current.identifier
        val rejectMsg = s"Another relay point with id '$identifier' is currently connected on the targeted network."
        import AmbiguityStrategy._
        configuration.relayIDAmbiguityStrategy match {
            case CLOSE_SERVER =>
                sendRefusedConnection(socket, rejectMsg + " Consequences: Closing Server...")
                broadcastMessage(true, "RelayServer will close your connection because of a critical error")
                close(CloseReason.INTERNAL_ERROR)

            case REJECT_NEW =>
                Console.err.println("Rejected connection of a client because it gave an already registered relay identifier.")
                sendRefusedConnection(socket, rejectMsg)

            case REPLACE =>
                connectionsManager.unregister(identifier).get.close(CloseReason.INTERNAL_ERROR)
                connectionsManager.registerConnection(identifier, socket)
            //The connection initialisation packet isn't sent here because it is send into the registerConnection method.

            case DISCONNECT_BOTH =>
                connectionsManager.unregister(identifier).get.close(CloseReason.INTERNAL_ERROR)
                sendRefusedConnection(socket, rejectMsg + " Consequences : Disconnected both")
        }
    }

    private[server] def sendAuthorisedConnection(socket: DynamicSocket): Unit = {
        val coordinates = DedicatedPacketCoordinates(PacketTraffic.SystemChannelID, "unknown", identifier)
        socket.write(packetTranslator.fromPacketAndCoords(BooleanPacket(true), coordinates))
    }

    private[server] def sendRefusedConnection(socket: DynamicSocket, message: String): Unit = {
        val coordinates = DedicatedPacketCoordinates(PacketTraffic.SystemChannelID, "unknown", identifier)
        socket.write(packetTranslator.fromPacketAndCoords(BooleanPacket(false), coordinates))
        socket.write(packetTranslator.fromPacketAndCoords(StringPacket(message), coordinates))
    }

}