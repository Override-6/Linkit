package fr.overridescala.vps.ftp.server

import java.net.{InetSocketAddress, ServerSocket, SocketException}
import java.nio.charset.Charset
import java.nio.file.Paths

import fr.overridescala.vps.ftp.api.`extension`.RelayExtensionLoader
import fr.overridescala.vps.ftp.api.`extension`.event.EventDispatcher
import fr.overridescala.vps.ftp.api.exceptions.RelayException
import fr.overridescala.vps.ftp.api.`extension`.packet.PacketManager
import fr.overridescala.vps.ftp.api.packet.{AsyncPacketChannel, PacketChannel, SyncPacketChannel}
import fr.overridescala.vps.ftp.api.task.{Task, TaskCompleterHandler}
import fr.overridescala.vps.ftp.api.utils.Constants
import fr.overridescala.vps.ftp.api.{Reason, Relay, RelayProperties}
import fr.overridescala.vps.ftp.server.connection.{ConnectionsManager, SocketContainer}

import scala.util.control.NonFatal

class RelayServer extends Relay {

    private val serverSocket = new ServerSocket(Constants.PORT)
    private val connectionsManager = new ConnectionsManager(this)
    //Awful thing, only for debugging, and easily switch from localhost to vps.
    private val taskFolderPath =
        if (System.getenv().get("COMPUTERNAME") == "PC_MATERIEL_NET") Paths.get("C:\\Users\\maxim\\Desktop\\Dev\\VPS\\ClientSide\\RelayExtensions")
        else Paths.get("RelayExtensions/")
    @volatile private var open = false

    /**
     * For safety, prefer Relay#identfier instead of Constants.SERVER_ID
     * */
    override val identifier: String = Constants.SERVER_ID

    override val eventDispatcher: EventDispatcher = new EventDispatcher
    override val extensionLoader = new RelayExtensionLoader(this, taskFolderPath)
    override val taskCompleterHandler = new TaskCompleterHandler
    override val properties: RelayProperties = new RelayProperties
    override val packetManager = new PacketManager(eventDispatcher.notifier)
    private val notifier = eventDispatcher.notifier

    override def scheduleTask[R](task: Task[R]): RelayTaskAction[R] = {
        ensureOpen()
        val targetIdentifier = task.targetID
        val connection = connectionsManager.getConnectionFromIdentifier(targetIdentifier)
        if (connection == null)
            throw new NoSuchElementException(s"Unknown or unregistered relay with identifier '$targetIdentifier'")

        val tasksHandler = connection.tasksHandler
        task.preInit(tasksHandler, identifier)
        notifier.onTaskScheduled(task)
        RelayTaskAction(task)
    }


    override def start(): Unit = {
        println("Current encoding is " + Charset.defaultCharset().name())
        println("Listening on port " + Constants.PORT)
        println("Computer name is " + System.getenv().get("COMPUTERNAME"))

        AsyncPacketChannel.launch(packetManager)
        extensionLoader.loadExtensions()

        println("Ready !")
        notifier.onReady()
        open = true
        while (open) awaitClientConnection()

        close(Reason.LOCAL_REQUEST)
    }


    override def createSyncChannel(linkedRelayID: String, id: Int): PacketChannel.Sync =
        createSync(linkedRelayID, id)

    override def createAsyncChannel(linkedRelayID: String, id: Int): PacketChannel.Async =
        createAsync(linkedRelayID, id)

    override def close(reason: Reason): Unit =
        close(identifier, reason)

    private[server] def createSync(linkedRelayID: String, id: Int): SyncPacketChannel = {
        val targetConnection = connectionsManager.getConnectionFromIdentifier(linkedRelayID)
        targetConnection.createSync(id)
    }

    private[server] def createAsync(linkedRelayID: String, id: Int): AsyncPacketChannel = {
        val targetConnection = connectionsManager.getConnectionFromIdentifier(linkedRelayID)
        targetConnection.createAsync(id)
    }

    def close(relayId: String, reason: Reason): Unit = {
        println("closing server...")
        connectionsManager.close(reason)
        serverSocket.close()
        open = false
        notifier.onClosed(relayId, reason)
        println("server closed !")
    }

    private def awaitClientConnection(): Unit = {
        try {
            val clientSocket = serverSocket.accept()
            val address = clientSocket.getRemoteSocketAddress.asInstanceOf[InetSocketAddress]
            val connection = connectionsManager.getConnectionFromAddress(address.getAddress.getHostAddress)
            if (connection == null) {
                val socketContainer = new SocketContainer(notifier)
                socketContainer.set(clientSocket)
                connectionsManager.register(socketContainer)
                return
            }
            connection.updateSocket(clientSocket)

        } catch {
            case e: RelayException =>
                Console.err.println(e.getMessage)
                notifier.onSystemError(e)
            case e: SocketException if e.getMessage == "Socket closed" =>
                Console.err.println(e.getMessage)
                close(Reason.ERROR_OCCURRED)
            case NonFatal(e) => e.printStackTrace()
        }
    }

    private def ensureOpen(): Unit = {
        if (!open)
            throw new UnsupportedOperationException("Relay Point have to be started !")
    }

    // default tasks
    Runtime.getRuntime.addShutdownHook(new Thread(() => close(Reason.LOCAL_REQUEST)))

}