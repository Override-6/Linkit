package fr.overridescala.vps.ftp.server

import java.net.{InetSocketAddress, ServerSocket, SocketException}
import java.nio.charset.Charset
import java.nio.file.{Path, Paths}

import fr.overridescala.vps.ftp.api.`extension`.RelayExtensionLoader
import fr.overridescala.vps.ftp.api.`extension`.packet.PacketManager
import fr.overridescala.vps.ftp.api.exceptions.{RelayClosedException, RelayException}
import fr.overridescala.vps.ftp.api.packet.fundamental.DataPacket
import fr.overridescala.vps.ftp.api.packet.{AsyncPacketChannel, PacketChannel, SyncPacketChannel}
import fr.overridescala.vps.ftp.api.system.{Reason, SystemOrder}
import fr.overridescala.vps.ftp.api.system.event.EventDispatcher
import fr.overridescala.vps.ftp.api.task.{Task, TaskCompleterHandler}
import fr.overridescala.vps.ftp.api.utils.Constants
import fr.overridescala.vps.ftp.api.{Relay, RelayProperties}
import fr.overridescala.vps.ftp.server.RelayServer.Identifier
import fr.overridescala.vps.ftp.server.connection.{ConnectionsManager, SocketContainer}

import scala.util.control.NonFatal

object RelayServer {
    val Identifier = "server"
}

class RelayServer extends Relay {

    private val serverSocket = new ServerSocket(Constants.PORT)
    private val taskFolderPath = getTasksFolderPath


    @volatile private var open = false
    /**
     * For safety, prefer Relay#identfier instead of Constants.SERVER_ID
     * */
    override val identifier: String = Identifier
    override val eventDispatcher: EventDispatcher = new EventDispatcher
    override val extensionLoader = new RelayExtensionLoader(this, taskFolderPath)
    override val taskCompleterHandler = new TaskCompleterHandler
    override val properties: RelayProperties = new RelayProperties
    override val packetManager = new PacketManager(eventDispatcher.notifier)

    private[server] val notifier = eventDispatcher.notifier
    val connectionsManager = new ConnectionsManager(this)

    override def scheduleTask[R](task: Task[R]): RelayTaskAction[R] = {
        ensureOpen()
        val targetIdentifier = task.targetID
        val connection = connectionsManager.getConnectionFromIdentifier(targetIdentifier)
        if (connection == null)
            throw new NoSuchElementException(s"Unknown or unregistered relay with identifier '$targetIdentifier'")

        val tasksHandler = connection.tasksHandler
        task.preInit(tasksHandler, identifier)
        notifier.onTaskScheduled(task)
        RelayTaskAction.of(task)
    }

    override def start(): Unit = {
        println("Current encoding is " + Charset.defaultCharset().name())
        println("Listening on port " + Constants.PORT)
        println("Computer name is " + System.getenv().get("COMPUTERNAME"))

        AsyncPacketChannel.UploadThread.start()
        extensionLoader.loadExtensions()

        println("Ready !")
        notifier.onReady()
        open = true
        while (open) awaitClientConnection()

        close(Reason.INTERNAL)
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
            //notifier.onSystemError(e.getType, Reason.LOCAL_ERROR)
            case e: SocketException if e.getMessage == "Socket closed" =>
                Console.err.println(e.getMessage)
                close(Reason.INTERNAL_ERROR)
            case NonFatal(e) => e.printStackTrace()
        }
    }

    private def getTasksFolderPath: Path = {
        val path = System.getenv().get("COMPUTERNAME") match {
            case "PC_MATERIEL_NET" => "C:\\Users\\maxim\\Desktop\\Dev\\VPS\\ClientSide\\RelayExtensions"
            case _ => "RelayExtensions/"
        }
        Paths.get(path)
    }

    private def ensureOpen(): Unit = {
        if (!open)
            throw new RelayClosedException("Relay Server have to be started !")
    }

    Runtime.getRuntime.addShutdownHook(new Thread(() => close(Reason.INTERNAL)))


}

// default tasks
